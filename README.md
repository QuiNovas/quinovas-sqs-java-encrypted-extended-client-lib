Quinovas SQS Encrypted Extended Client Library for Java
===========================================
The **Quinovas SQS Encrypted Extended Client Library for Java** enables you to manage Amazon SQS message encrypted payloads with Amazon S3. This is especially useful for storing and retrieving messages with a message payload size greater than the current SQS limit of 256 KB, up to a maximum of 2 GB. Specifically, you can use this library to:

* It's encuraged to set the maximum message size to 64 KB as larger messages even though SQS supports up to 256 KB. Each 64 KB chunk of a payload is billed as 1 request (for example, an API action with a 256 KB payload is billed as 4 requests).
* Specify whether message payloads are always stored in Amazon S3 or only when a message's size exceeds 256 KB.

* Send a message that references a single message object stored in an Amazon S3 bucket.

* Get the corresponding message object from an Amazon S3 bucket.

* Delete the corresponding message object from an Amazon S3 bucket.


You can download release builds through the [releases section of this](https://github.com/QuiNovas/quinovas-sqs-java-encrypted-extended-client-lib) project.

For more information on using the amazon-sqs-java-extended-client-lib, see our getting started guide [here](http://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/s3-messages.html).

## JAVA Example

* Configuration items needed for example:
  - aws access key (aws.access_key_id)
  - aws secret access key (aws.secret_access_key)
  - aws region (aws.region)
  - s3 bucket name (messagesBucket)
  - kms key (aws.kms_key)
  - group ID (groupID)
  - SQS queue name (outboundQueueName)


```java
    private SQSConnection getConnection(Properties props) throws JMSException {
        // Create the connection factory based on the config
        final BasicAWSCredentials profileCredentials = new BasicAWSCredentials(props.getProperty("aws.access_key_id"),
                props.getProperty("aws.secret_access_key"));

        final Regions region = Regions.fromName(props.getProperty("aws.region"));

        final AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion(region)
                .withCredentials(new AWSStaticCredentialsProvider(profileCredentials)).build();

        final String S3_BUCKET_NAME = props.getProperty("messagesBucket");

        final QuinovasExtendedClientConfiguration extendedClientConfig = new QuinovasExtendedClientConfiguration()
                .withLargePayloadSupportEnabled(s3, S3_BUCKET_NAME).withMessageSizeThreshold(64 * 1024)
                .withKeyAlias(props.getProperty("aws.kms_key"));

        final AmazonSQS sqsClient = AmazonSQSClientBuilder.standard().withRegion(region)
                .withCredentials(new AWSStaticCredentialsProvider(profileCredentials)).build();

        final AmazonSQS sqsExtended = new QuinovasSQSEncryptedExtendedClient(sqsClient, extendedClientConfig);

        final SQSConnectionFactory connectionFactory = new SQSConnectionFactory(new ProviderConfiguration(),
                sqsExtended);

        return connectionFactory.createConnection();
    }

    private TextMessage createMessage(Boolean large, Session session, Properties props, Boolean sendUniqueMessage) throws JMSException {
        String messageBody = "There are two hard things in computer science: cache invalidation, naming things, and off-by-one errors.";

        if (large) {
            final int stringLength = 100000;
            final char[] chars = new char[stringLength];
            Arrays.fill(chars, 'a');
            messageBody = new String(chars);
        }

        if (sendUniqueMessage) {
            messageBody += UUID.randomUUID().toString();
        }

        final TextMessage message = session.createTextMessage(messageBody);
        message.setJMSCorrelationID(UUID.randomUUID().toString());
        message.setStringProperty("JMSXGroupID", props.getProperty("groupID"));
        return message;
    }

    public void sendJMSMessage() {
        final Properties props = getConfig(propertyFilePath);
        try {
            SQSConnection connection = getConnection(props);
            final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            final MessageProducer producer = session
                    .createProducer(session.createQueue(props.getProperty("outboundQueueName")));
            final TextMessage message = createMessage(false, session, props, true);
            producer.send(message);
            System.out.println("Send message " + message.getJMSMessageID());
            // Close the connection. This closes the session automatically
            connection.close();
        } catch (final JMSException e) {
            System.err.println("Failed reading input: " + e.getMessage());
        }

        System.out.println("Connection closed");
    }
```
## Getting Started

* Maven:
```xml
    <dependency>
      <groupId>com.amazonaws</groupId>
      <artifactId>amazon-sqs-java-extended-client-lib</artifactId>
      <version>1.0.2</version>
      <type>jar</type>
    </dependency>
    <dependency>
      <groupId>com.amazonaws</groupId>
      <artifactId>aws-java-sdk-s3</artifactId>
      <version>1.11.615</version>
    </dependency>
    <dependency>
      <groupId>com.amazonaws</groupId>
      <artifactId>amazon-sqs-java-messaging-lib</artifactId>
      <version>1.0.8</version>
      <type>jar</type>
    </dependency>
```
* **Further information** - Read the [API documentation](http://aws.amazon.com/documentation/sqs/).

## Feedback
* Give us feedback [here](https://github.com/QuiNovas/quinovas-sqs-java-encrypted-extended-client-lib/issues).