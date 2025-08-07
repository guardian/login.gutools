package config

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{AWSCredentialsProviderChain, EnvironmentVariableCredentialsProvider, InstanceProfileCredentialsProvider, SystemPropertiesCredentialsProvider}
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.autoscaling.model.{DescribeAutoScalingGroupsRequest, DescribeAutoScalingInstancesRequest}
import com.amazonaws.services.autoscaling.{AmazonAutoScaling, AmazonAutoScalingClientBuilder}
import com.amazonaws.services.simpleemail.{AmazonSimpleEmailService, AmazonSimpleEmailServiceClientBuilder}
import com.amazonaws.services.simplesystemsmanagement.{AWSSimpleSystemsManagement, AWSSimpleSystemsManagementClientBuilder}
import com.amazonaws.util.EC2MetadataUtils
import software.amazon.awssdk.auth.credentials.{AwsCredentialsProviderChain, EnvironmentVariableCredentialsProvider => EnvironmentVariableCredentialsProviderV2, InstanceProfileCredentialsProvider => InstanceProfileCredentialsProviderV2, ProfileCredentialsProvider => ProfileCredentialsProviderV2}
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder
import software.amazon.awssdk.regions.{Region => RegionV2}
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.s3.{S3AsyncClient, S3Client}

import scala.jdk.CollectionConverters._

case class InstanceTags(stack: String, app: String, stage: String)

class AWS {
  val region = Region.getRegion(Regions.EU_WEST_1).getName
  val profile = "composer"

  val composerAwsCredentialsProvider = new AWSCredentialsProviderChain(
    new EnvironmentVariableCredentialsProvider(),
    new SystemPropertiesCredentialsProvider(),
    InstanceProfileCredentialsProvider.getInstance(),
    new ProfileCredentialsProvider(profile)
  )

  val asgClient: AmazonAutoScaling = AmazonAutoScalingClientBuilder.standard().withRegion(region).withCredentials(composerAwsCredentialsProvider).build()
  val ssmClient: AWSSimpleSystemsManagement = AWSSimpleSystemsManagementClientBuilder.standard().withRegion(region).withCredentials(composerAwsCredentialsProvider).build()
  val sesClient: AmazonSimpleEmailService = AmazonSimpleEmailServiceClientBuilder.standard().withRegion(region).withCredentials(composerAwsCredentialsProvider).build()

  private val v2CredentialsProvider = AwsCredentialsProviderChain.builder.credentialsProviders(
    ProfileCredentialsProviderV2.builder.profileName(profile).build,
    InstanceProfileCredentialsProviderV2.builder.asyncCredentialUpdateEnabled(false).build,
    EnvironmentVariableCredentialsProviderV2.create()
  ).build

  private def setup[B <: AwsClientBuilder[B, _]](builder: B): B =
    builder.credentialsProvider(v2CredentialsProvider).region(RegionV2.EU_WEST_1)

  val s3SyncClient: S3Client = setup(S3Client.builder).build()
  val s3AsyncClient: S3AsyncClient = setup(S3AsyncClient.builder).build()
  val dynamoDbClient: DynamoDbClient = setup(DynamoDbClient.builder).build()

  def readTags(): Option[InstanceTags] = {
    // We read tags from the AutoScalingGroup rather than the instance itself to avoid problems where the
    // tags have not been applied to the instance before we start up (they are eventually consistent)
    for {
      instanceId <- Option(EC2MetadataUtils.getInstanceId)
      asg <- getAutoscalingGroupName(instanceId)
      tags <- getTags(asg)

      stack <- tags.get("Stack")
      app <- tags.get("App")
      stage <- tags.get("Stage")
    } yield {
      InstanceTags(stack, app, stage)
    }
  }

  private def getAutoscalingGroupName(instanceId: String): Option[String] = {
    val request = new DescribeAutoScalingInstancesRequest().withInstanceIds(instanceId)
    val response = asgClient.describeAutoScalingInstances(request)

    val instance = response.getAutoScalingInstances.asScala.headOption
    instance.map(_.getAutoScalingGroupName)
  }

  private def getTags(autoscalingGroupName: String): Option[Map[String, String]] = {
    val request = new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(autoscalingGroupName)
    val response = asgClient.describeAutoScalingGroups(request)

    val maybeGroup = response.getAutoScalingGroups.asScala.headOption
    maybeGroup.map {
      _.getTags
        .asScala
        .map { t => t.getKey -> t.getValue }
        .toMap
    }
  }
}
