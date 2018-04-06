package config

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{AWSCredentialsProviderChain, EnvironmentVariableCredentialsProvider, InstanceProfileCredentialsProvider, SystemPropertiesCredentialsProvider}
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder
import com.amazonaws.services.ec2.model.{DescribeTagsRequest, Filter}
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder
import com.amazonaws.util.EC2MetadataUtils

import scala.collection.JavaConverters._


object AWS {
  lazy val region = Region.getRegion(Regions.EU_WEST_1).getName
  lazy val workflowAwsCredentialsProvider = new AWSCredentialsProviderChain(
    new EnvironmentVariableCredentialsProvider(),
    new SystemPropertiesCredentialsProvider(),
    InstanceProfileCredentialsProvider.getInstance(),
    new ProfileCredentialsProvider("workflow")
  )
  lazy val composerAwsCredentialsProvider = new AWSCredentialsProviderChain(
    new EnvironmentVariableCredentialsProvider(),
    new SystemPropertiesCredentialsProvider(),
    InstanceProfileCredentialsProvider.getInstance(),
    new ProfileCredentialsProvider("composer")
  )

  lazy val ec2Client = AmazonEC2ClientBuilder.standard().withRegion(region).withCredentials(workflowAwsCredentialsProvider).build()
  lazy val s3Client = AmazonS3ClientBuilder.standard().withRegion(region).withCredentials(composerAwsCredentialsProvider).build()
  lazy val sesClient = AmazonSimpleEmailServiceClientBuilder.standard().withRegion(region).withCredentials(composerAwsCredentialsProvider).build()
  lazy val dynamoDbClient = AmazonDynamoDBClientBuilder.standard().withRegion(region).withCredentials(composerAwsCredentialsProvider).build()

  def readTag(tagName: String): Option[String] = {
    Option(EC2MetadataUtils.getInstanceId).flatMap { id =>
      val tagsResult = ec2Client.describeTags(
        new DescribeTagsRequest().withFilters(
          new Filter("resource-type").withValues("instance"),
          new Filter("resource-id").withValues(id),
          new Filter("key").withValues(tagName)
        )
      )
      tagsResult.getTags.asScala
        .find(_.getKey == tagName)
        .map(_.getValue)
    }
  }
}
