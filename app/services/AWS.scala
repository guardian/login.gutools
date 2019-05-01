package services

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{AWSCredentialsProviderChain, EnvironmentVariableCredentialsProvider, InstanceProfileCredentialsProvider, SystemPropertiesCredentialsProvider}
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder
import com.amazonaws.services.autoscaling.model.{DescribeAutoScalingGroupsRequest, DescribeAutoScalingInstancesRequest}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder
import com.amazonaws.util.EC2MetadataUtils

import scala.collection.JavaConverters._

case class InstanceTags(stack: String, app: String, stage: String)

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

  lazy val asgClient = AmazonAutoScalingClientBuilder.standard().withRegion(region).withCredentials(composerAwsCredentialsProvider).build()
  lazy val ssmClient = AWSSimpleSystemsManagementClientBuilder.standard().withRegion(region).withCredentials(composerAwsCredentialsProvider).build()

  lazy val s3Client = AmazonS3ClientBuilder.standard().withRegion(region).withCredentials(composerAwsCredentialsProvider).build()
  lazy val sesClient = AmazonSimpleEmailServiceClientBuilder.standard().withRegion(region).withCredentials(composerAwsCredentialsProvider).build()
  lazy val dynamoDbClient = AmazonDynamoDBClientBuilder.standard().withRegion(region).withCredentials(composerAwsCredentialsProvider).build()

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

    val group = response.getAutoScalingGroups.asScala.headOption
    group.map(_.getTags.asScala.map { t => t.getKey -> t.getValue }(scala.collection.breakOut))
  }
}
