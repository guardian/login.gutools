package config

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{AWSCredentialsProviderChain, EnvironmentVariableCredentialsProvider, InstanceProfileCredentialsProvider, SystemPropertiesCredentialsProvider}
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ec2.model.{DescribeTagsRequest, Filter}
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.util.EC2MetadataUtils

import scala.collection.JavaConverters._


object AWS {
  lazy val region = Region getRegion Regions.EU_WEST_1
  lazy val workflowAwsCredentialsProvider = new AWSCredentialsProviderChain(
    new EnvironmentVariableCredentialsProvider(),
    new SystemPropertiesCredentialsProvider(),
    new InstanceProfileCredentialsProvider(),
    new ProfileCredentialsProvider("workflow")
  )
  lazy val composerAwsCredentialsProvider = new AWSCredentialsProviderChain(
    new EnvironmentVariableCredentialsProvider(),
    new SystemPropertiesCredentialsProvider(),
    new InstanceProfileCredentialsProvider(),
    new ProfileCredentialsProvider("composer")
  )

  def eC2Client = region.createClient(classOf[AmazonEC2Client], workflowAwsCredentialsProvider, null)
  def s3Client = region.createClient(classOf[AmazonS3Client], composerAwsCredentialsProvider, null)
  def dynamoDbClient = region.createClient(classOf[AmazonDynamoDBClient], composerAwsCredentialsProvider, null)
  val tokenDynamoTable = s"reissue-tokens-CODE"
  val emegergencyAccessDynamo = s"login.gutools-emergency-access-CODE"

  def getInstanceId = Option(EC2MetadataUtils.getInstanceId)

  def readTag(tagName: String, instanceId: Option[String])(implicit ec2Client: AmazonEC2Client): Option[String] = {
    instanceId.flatMap { id =>
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
