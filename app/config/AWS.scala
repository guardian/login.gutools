package config

import software.amazon.awssdk.services.autoscaling.model.{DescribeAutoScalingGroupsRequest, DescribeAutoScalingInstancesRequest}
import software.amazon.awssdk.services.autoscaling.AutoScalingClient
import software.amazon.awssdk.services.ses.SesClient
import software.amazon.awssdk.services.ssm.SsmClient
import software.amazon.awssdk.regions.internal.util.EC2MetadataUtils
import com.gu.pandomainauth.S3BucketLoader
import software.amazon.awssdk.auth.credentials.{AwsCredentialsProviderChain, EnvironmentVariableCredentialsProvider, InstanceProfileCredentialsProvider, ProfileCredentialsProvider, SystemPropertyCredentialsProvider}
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.{S3AsyncClient, S3Client}

import scala.jdk.CollectionConverters._
import scala.util.Try

case class InstanceTags(stack: String, app: String, stage: String)

class AWS {
  val region = Region.EU_WEST_1
  val profile = "composer"


  private val v2CredentialsProvider = AwsCredentialsProviderChain.builder.credentialsProviders(
    ProfileCredentialsProvider.builder.profileName(profile).build,
    InstanceProfileCredentialsProvider.builder.asyncCredentialUpdateEnabled(false).build,
    SystemPropertyCredentialsProvider.create(),
    EnvironmentVariableCredentialsProvider.create()
  ).build

  private def setup[B <: AwsClientBuilder[B, _]](builder: B): B =
    builder.credentialsProvider(v2CredentialsProvider).region(region)
  val s3SyncClient: S3Client = setup(S3Client.builder).build()
  val s3AsyncClient: S3AsyncClient = setup(S3AsyncClient.builder).build()
  val dynamoDbClient: DynamoDbClient = setup(DynamoDbClient.builder).build()
  val asgClient: AutoScalingClient = setup(AutoScalingClient.builder).build()
  val ssmClient: SsmClient = setup(SsmClient.builder).build()
  val sesClient: SesClient = setup(SesClient.builder).build()

  def readTags(): Option[InstanceTags] = {
    // We read tags from the AutoScalingGroup rather than the instance itself to avoid problems where the
    // tags have not been applied to the instance before we start up (they are eventually consistent)
    for {
      instanceId <-  Try(EC2MetadataUtils.getInstanceId).toOption
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
    val request = DescribeAutoScalingInstancesRequest.builder.instanceIds(instanceId).build()
    val response = asgClient.describeAutoScalingInstances(request)

    val instance = response.autoScalingInstances.asScala.headOption
    instance.map(_.autoScalingGroupName)
  }

  private def getTags(autoscalingGroupName: String): Option[Map[String, String]] = {
    val request = DescribeAutoScalingGroupsRequest.builder.autoScalingGroupNames(autoscalingGroupName).build()
    val response = asgClient.describeAutoScalingGroups(request)

    val maybeGroup = response.autoScalingGroups.asScala.headOption

    maybeGroup.map {
      _.tags
        .asScala
        .map { t => t.key -> t.value }
        .toMap
    }
  }
}
