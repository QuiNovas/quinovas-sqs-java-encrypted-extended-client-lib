package com.quinovas;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.UUID;

import com.amazon.sqs.javamessaging.ExtendedClientConfiguration;
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

        // Create a message queue for this example.
        //final String QueueName = "MyQueue" + UUID.randomUUID().toString();
        //final CreateQueueRequest createQueueRequest =
        //        new CreateQueueRequest(QueueName);
        //final String myQueueUrl = sqsExtended
        //        .createQueue(createQueueRequest).getQueueUrl();
        //System.out.println("Queue created.");

        // Send the message.
        //"https://queue.amazonaws.com/527681936864/Roger-inbound.fifo", 

        final SendMessageRequest myMessageRequest =
                new SendMessageRequest(
                    "https://queue.amazonaws.com/527681936864/Roger-inbound.fifo", 
                    myLongString);
        myMessageRequest.setMessageGroupId("test-message-group-id");
        sqsExtended.sendMessage(myMessageRequest);
        System.out.println("Sent the message.");                        
        assertTrue(true);
    }
    
}
