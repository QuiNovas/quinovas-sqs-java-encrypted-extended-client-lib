package com.quinovas;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import com.amazon.sqs.javamessaging.ExtendedClientConfiguration;
import com.amazon.sqs.javamessaging.ProviderConfiguration;
import com.amazon.sqs.javamessaging.SQSConnection;
import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import com.amazon.sqs.javamessaging.SQSQueueDestination;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Builder;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.SSEAwsKeyManagementParams;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazon.sqs.javamessaging.AmazonSQSExtendedClient;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.util.StringUtils;
import com.quinovas.*;

import org.apache.http.io.SessionOutputBuffer;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest {
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue() {
        assertTrue(true);
    }

    private final String propertyFilePath= "src/test/resources/config.properties";

    private Properties getConfig(final String file) {
        BufferedReader reader;
        Properties properties;
        try {
            reader = new BufferedReader(new FileReader(file));
            properties = new Properties();
            try {
                properties.load(reader);
                reader.close();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        } catch (final FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("Configuration.properties not found at " + propertyFilePath);
        }

        return properties;
    }

    @Test
    public void getProperties() {
        final Properties props = getConfig(propertyFilePath);
        assertTrue(!StringUtils.isNullOrEmpty(props.getProperty("aws.access_key_id")));
        assertTrue(!StringUtils.isNullOrEmpty(props.getProperty("aws.secret_access_key")));
        assertTrue(!StringUtils.isNullOrEmpty(props.getProperty("aws.region")));
        assertTrue(!StringUtils.isNullOrEmpty(props.getProperty("autotec.outboundQueueName")));
        assertTrue(!StringUtils.isNullOrEmpty(props.getProperty("autotec.inboundQueueName")));
        assertTrue(!StringUtils.isNullOrEmpty(props.getProperty("autotec.keyId")));
        assertTrue(!StringUtils.isNullOrEmpty(props.getProperty("autotec.messagesBucket")));
    }

    @Test
    public void testSendMessage() {
        final BasicAWSCredentials profileCredentials = new BasicAWSCredentials("AKIAXVXCLLXQLLEJ53WN",
                "jmXLL6BqYui22DvU9UWks8zXWqUXdDrEvxXwttjm");
        final AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_1)
                .withCredentials(new AWSStaticCredentialsProvider(profileCredentials)).build();

        final byte[] plaintext = "Hello S3/KMS SSE Encryption!".getBytes(Charset.forName("UTF-8"));
        final ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(plaintext.length);

        final String bucket = "autotec-dev-messaging";
        final PutObjectRequest req = new PutObjectRequest(bucket, "hello_s3_sse_kms.txt",
                new ByteArrayInputStream(plaintext), metadata)
                        .withSSEAwsKeyManagementParams(new SSEAwsKeyManagementParams());

        final PutObjectResult putResult = s3.putObject(req);
        System.out.println(putResult);
        assertTrue(true);
    }

    @Test
    public void SendSQSMessage() {
        final Properties props = getConfig(propertyFilePath);
        final BasicAWSCredentials profileCredentials = new BasicAWSCredentials(props.getProperty("aws.access_key_id"),
            props.getProperty("aws.secret_access_key"));
        final AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_1)
                .withCredentials(new AWSStaticCredentialsProvider(profileCredentials)).build();

        final String S3_BUCKET_NAME = props.getProperty("autotec.messagesBucket");

        final QuinovasExtendedClientConfiguration extendedClientConfig = new QuinovasExtendedClientConfiguration()
                .withLargePayloadSupportEnabled(s3, S3_BUCKET_NAME)
                .withMessageSizeThreshold(64 * 1024)
                .withKeyAlias(props.getProperty("autotec.keyId"));

        final AmazonSQS sqsClient = AmazonSQSClientBuilder.standard().withRegion(Regions.US_EAST_1)
                .withCredentials(new AWSStaticCredentialsProvider(profileCredentials)).build();

        final AmazonSQS sqsExtended = new QuinovasSQSEncryptedExtendedClient(sqsClient, extendedClientConfig);
        /*
         * Create a long string of characters for the message object which will be
         * stored in the bucket.
         */
        final int stringLength = 300000;
        final char[] chars = new char[stringLength];
        Arrays.fill(chars, 'x');
        final String myLongString = new String(chars);

        final SendMessageRequest myMessageRequest = new SendMessageRequest(
            props.getProperty("autotec.outboundQueueUrl"), myLongString);
        myMessageRequest.setMessageGroupId("test-message-group-id");
        // myMessageRequest.set
        sqsExtended.sendMessage(myMessageRequest);
        System.out.println("Sent the message.");
        assertTrue(true);
    }

    @Test
    public void SendJMSMessage() {
        final Properties props = getConfig(propertyFilePath);
        // Create the connection factory based on the config
        final BasicAWSCredentials profileCredentials = new BasicAWSCredentials(props.getProperty("aws.access_key_id"),
            props.getProperty("aws.secret_access_key"));
                    
        final Regions region = Regions.fromName(props.getProperty("aws.region"));

        final AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion(region)
                .withCredentials(new AWSStaticCredentialsProvider(profileCredentials)).build();

        final String S3_BUCKET_NAME = props.getProperty("autotec.messagesBucket");

        final QuinovasExtendedClientConfiguration extendedClientConfig = new QuinovasExtendedClientConfiguration()
                .withLargePayloadSupportEnabled(s3, S3_BUCKET_NAME)
                .withMessageSizeThreshold(64 * 1024)
                .withKeyAlias(props.getProperty("autotec.keyId"));

        final AmazonSQS sqsClient = AmazonSQSClientBuilder.standard().withRegion(region)
                .withCredentials(new AWSStaticCredentialsProvider(profileCredentials)).build();

        final AmazonSQS sqsExtended = new QuinovasSQSEncryptedExtendedClient(sqsClient, extendedClientConfig);

        final SQSConnectionFactory connectionFactory = new SQSConnectionFactory(new ProviderConfiguration(),
                sqsExtended);

        // Create the connection
        SQSConnection connection;
        try {
            connection = connectionFactory.createConnection();
            final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            final MessageProducer producer = session.createProducer(session.createQueue(props.getProperty("autotec.outboundQueueName")));
            final TextMessage message = session.createTextMessage(
                    "There are two hard things in computer science: cache invalidation, naming things, and off-by-one errors.");
            message.setJMSCorrelationID("correlationID");
            message.setStringProperty("JMSXGroupID", props.getProperty("autotec.auctionId"));
            message.setStringProperty("AuctionID", props.getProperty("autotec.auctionId"));
            message.setStringProperty("AAMessageType", "somemessagetype");
            producer.send(message);
            System.out.println("Send message " + message.getJMSMessageID());
            // Close the connection. This closes the session automatically
            connection.close();
        } catch (final JMSException e) {
            System.err.println( "Failed reading input: " + e.getMessage() );
        }
                    
        // Create the session
        System.out.println( "Connection closed" );
        assertTrue(true);
    }

    @Test
    public void ReadJMSMessage() {
        final Properties props = getConfig(propertyFilePath);

        // Create the connection factory based on the config
        final BasicAWSCredentials profileCredentials = new BasicAWSCredentials(props.getProperty("aws.access_key_id"),
            props.getProperty("aws.secret_access_key"));
                    
        final Regions region = Regions.fromName(props.getProperty("aws.region"));

        final AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion(region)
                .withCredentials(new AWSStaticCredentialsProvider(profileCredentials)).build();

        final String S3_BUCKET_NAME = props.getProperty("autotec.messagesBucket");

        final QuinovasExtendedClientConfiguration extendedClientConfig = new QuinovasExtendedClientConfiguration()
                .withLargePayloadSupportEnabled(s3, S3_BUCKET_NAME)
                .withMessageSizeThreshold(64 * 1024)
                .withKeyAlias(props.getProperty("autotec.keyId"));

        final AmazonSQS sqsClient = AmazonSQSClientBuilder.standard().withRegion(region)
                .withCredentials(new AWSStaticCredentialsProvider(profileCredentials)).build();

        final AmazonSQS sqsExtended = new QuinovasSQSEncryptedExtendedClient(sqsClient, extendedClientConfig);

        final SQSConnectionFactory connectionFactory = new SQSConnectionFactory(new ProviderConfiguration(),
                sqsExtended);

        // Create the connection
        SQSConnection connection;
        try {
            connection = connectionFactory.createConnection();
            final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            final MessageConsumer consumer = session.createConsumer(session.createQueue(props.getProperty("autotec.inboundQueueName")));
            connection.start();
            receiveMessages(session, consumer);
            connection.close();
        } catch (final JMSException e) {
            System.err.println("Failed reading input: " + e.getMessage());
        }
                    
        // Create the session
        System.out.println( "Connection closed" );
        assertTrue(true);
    }

    private static void receiveMessages( Session session, MessageConsumer consumer ) {
        try {
            System.out.println("Waiting for messages");
            Message message = consumer.receive(TimeUnit.MINUTES.toMillis(1));
            if(message == null) {
                System.out.println("Shutting down after 1 minute of silence");
            } 
            System.out.println(message.getJMSCorrelationID());
            System.out.println(message.getJMSMessageID());
            System.out.println(message.getStringProperty("AuctionID"));

            message.acknowledge();
            System.out.println("Acknowledged message " + message.getJMSMessageID());
        } catch (JMSException e) {
            System.err.println("Error receiving from SQS: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
