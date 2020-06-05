package com.quinovas;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.UUID;

import javax.jms.JMSException;
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
import com.quinovas.*;
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

    @Test
    public void testSendMessage() {
        final BasicAWSCredentials profileCredentials = new BasicAWSCredentials("AKIAXVXCLLXQLLEJ53WN", "jmXLL6BqYui22DvU9UWks8zXWqUXdDrEvxXwttjm");
        final AmazonS3 s3 = AmazonS3ClientBuilder.standard()
            .withRegion(Regions.US_EAST_1)
            .withCredentials(new AWSStaticCredentialsProvider(profileCredentials))
            .build();

        final byte[] plaintext = "Hello S3/KMS SSE Encryption!"
            .getBytes(Charset.forName("UTF-8"));
        final ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(plaintext.length);

        final String bucket = "autotec-dev-messaging";
        final PutObjectRequest req = new PutObjectRequest(bucket,
                "hello_s3_sse_kms.txt",
            new ByteArrayInputStream(plaintext), metadata)
                .withSSEAwsKeyManagementParams(
            new SSEAwsKeyManagementParams());
        
        final PutObjectResult putResult = s3.putObject(req);
        System.out.println(putResult);

        //s3.putObject("autotec-dev-messaging", "my-test-key", "some object");
        //    .with

            //.withSSEAwsKeyManagementParams(new SSEAwsKeyManagementParams());
        ;
        assertTrue(true);
    }

    @Test
    public void SendSQSMessage() {
        final BasicAWSCredentials profileCredentials = new BasicAWSCredentials("AKIAXVXCLLXQLLEJ53WN", "jmXLL6BqYui22DvU9UWks8zXWqUXdDrEvxXwttjm");
        final AmazonS3 s3 = AmazonS3ClientBuilder.standard()
            .withRegion(Regions.US_EAST_1)
            .withCredentials(new AWSStaticCredentialsProvider(profileCredentials))
            .build();

        final String S3_BUCKET_NAME = "autotec-dev-messaging";
            

        final ExtendedClientConfiguration extendedClientConfig =
                new ExtendedClientConfiguration()
                        .withLargePayloadSupportEnabled(s3, S3_BUCKET_NAME);

            
        final AmazonSQS sqsClient = AmazonSQSClientBuilder.standard().withRegion(Regions.US_EAST_1).withCredentials(new AWSStaticCredentialsProvider(profileCredentials)).build();
              
        final AmazonSQS sqsExtended = new QuinovasSQSEncryptedExtendedClient(
                sqsClient, extendedClientConfig);

                //final AmazonSQS sqsExtended2 = new AmazonSQSExtendedClient(
               //     sqsClient, extendedClientConfig);                
        /*
         * Create a long string of characters for the message object which will
         * be stored in the bucket.
         */
        int stringLength = 300000;
        char[] chars = new char[stringLength];
        Arrays.fill(chars, 'x');
        final String myLongString = new String(chars);


        final SendMessageRequest myMessageRequest =
                new SendMessageRequest(
                    "https://queue.amazonaws.com/527681936864/Roger-inbound.fifo", 
                    myLongString);
        myMessageRequest.setMessageGroupId("test-message-group-id");
        //myMessageRequest.set
        sqsExtended.sendMessage(myMessageRequest);
        System.out.println("Sent the message.");                        
        assertTrue(true);
    }

    @Test
    public void SendJMSMessage() {
        // Create the connection factory based on the config  
        final BasicAWSCredentials profileCredentials = new BasicAWSCredentials("AKIAXVXCLLXQLLEJ53WN", "jmXLL6BqYui22DvU9UWks8zXWqUXdDrEvxXwttjm");
        final AmazonS3 s3 = AmazonS3ClientBuilder.standard()
            .withRegion(Regions.US_EAST_1)
            .withCredentials(new AWSStaticCredentialsProvider(profileCredentials))
            .build();

        final String S3_BUCKET_NAME = "autotec-dev-messaging";
            

        final ExtendedClientConfiguration extendedClientConfig =
                new ExtendedClientConfiguration()
                        .withLargePayloadSupportEnabled(s3, S3_BUCKET_NAME);
            
        final AmazonSQS sqsClient = AmazonSQSClientBuilder.standard().withRegion(Regions.US_EAST_1).withCredentials(new AWSStaticCredentialsProvider(profileCredentials)).build();
              
        final AmazonSQS sqsExtended = new QuinovasSQSEncryptedExtendedClient(
                sqsClient, extendedClientConfig);

        SQSConnectionFactory connectionFactory = new SQSConnectionFactory(
                new ProviderConfiguration(),
                sqsExtended);
        
        // Create the connection
        SQSConnection connection;
        try {
            connection = connectionFactory.createConnection();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(session.createQueue("Roger-inbound.fifo"));
            TextMessage message = session.createTextMessage("There are two hard things in computer science: cache invalidation, naming things, and off-by-one errors.");
            message.setJMSCorrelationID("correlationID");
            message.setStringProperty("JMSXGroupID", "Default");
            producer.send(message);
            System.out.println( "Send message " + message.getJMSMessageID() );
            // Close the connection. This closes the session automatically
            connection.close();
        } catch (JMSException e) {
            System.err.println( "Failed reading input: " + e.getMessage() );
        }
                    
        // Create the session
        System.out.println( "Connection closed" );
        assertTrue(true);
    }
}
