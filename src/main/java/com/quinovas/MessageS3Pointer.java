package com.quinovas;

import java.io.IOException;
import java.util.LinkedHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class is used for carrying pointer to Amazon S3 objects which contain
 * message payloads. For a large-payload messages, an instance of this class
 * will be serialized to JSON and sent through Amazon SQS.
 * 
 */
public class MessageS3Pointer {
	private String s3BucketName;
	private String s3Key;

	public MessageS3Pointer() {
	}

	public MessageS3Pointer(final String s3BucketName, final String s3Key) {
		this.s3BucketName = s3BucketName;
		this.s3Key = s3Key;
	}

	public static MessageS3Pointer FromS3JsonString(final String json) {
		final ObjectMapper mapper = new ObjectMapper();
		// json looks like this;
		// ["com.amazon.sqs.javamessaging.MessageS3Pointer",{"s3BucketName":"autotec-dev-messaging","s3Key":"988ab98b-d0ae-4cd2-9a95-7478f2486aad"}]
		// convert JSON file to Object Array
		try {
			Object[] map = mapper.readValue(json, Object[].class);
			final LinkedHashMap<String, String> mapperer = (LinkedHashMap<String, String>) map[1];
			return new MessageS3Pointer(mapperer.get("s3BucketName"), mapperer.get("s3Key"));
		} catch (final IOException e) {
				e.printStackTrace();
		}
		// The second object in the array is a linked hash map
		return new MessageS3Pointer();
	}

	public String getS3BucketName() {
		return s3BucketName;
	}

	public void setS3BucketName(final String s3BucketName) {
		this.s3BucketName = s3BucketName;
	}

	public String getS3Key() {
		return s3Key;
	}

	public void setS3Key(final String s3Key) {
		this.s3Key = s3Key;
	}

	@Override
	public String toString() {
		// faking using com.amazon.sqs.javamessaging.MessageS3Pointer, since this class was private
		return String.format("[\"com.amazon.sqs.javamessaging.MessageS3Pointer\",{\"s3BucketName\":\"%s\",\"s3Key\":\"%s\"}]", getS3BucketName(), getS3Key());
	}

}