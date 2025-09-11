package tech.aurora.bfadmin.service.impl;

import build.buildfarm.v1test.AdminGrpc;
import build.buildfarm.v1test.GetClientStartTimeRequest;
import build.buildfarm.v1test.GetClientStartTimeResult;
import build.buildfarm.v1test.GetClientStartTime;
import build.buildfarm.v1test.StopContainerRequest;
import build.buildfarm.v1test.TerminateHostRequest;
import build.buildfarm.v1test.ReindexCasRequest;

import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsResponse;
import software.amazon.awssdk.services.autoscaling.model.TagDescription;
import software.amazon.awssdk.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import software.amazon.awssdk.services.autoscaling.model.UpdateAutoScalingGroupResponse;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.Tag;

import software.amazon.awssdk.regions.Region;
import com.google.rpc.Status;
import tech.aurora.bfadmin.model.Asg;
import tech.aurora.bfadmin.model.ClusterDetails;
import tech.aurora.bfadmin.model.ClusterInfo;
import tech.aurora.bfadmin.model.Instance;
import tech.aurora.bfadmin.service.AdminService;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class AdminServiceImpl implements AdminService {
  private static final Logger logger = LoggerFactory.getLogger(AdminServiceImpl.class);

  @Value("${buildfarm.cluster.name}")
  private String clusterId;

  @Value("${buildfarm.worker.port}")
  private int workerPort;

  @Value("${aws.region}")
  private String region;

  @Value("${deployment.domain}")
  private String deploymentDomain;

  @Value("${buildfarm.public.port}")
  private int deploymentPort;

  private Ec2Client ec2;
  private AutoScalingClient autoScale;

  @PostConstruct
  public void init() {
    logger.info("Using AWS region: {}", region);
    ec2 = Ec2Client.builder().region(Region.of(region)).build();
    autoScale = AutoScalingClient.builder().region(Region.of(region)).build();
  }

  @Override
  public List<String> getAllClusters() {
    List<String> clusters = new ArrayList<>();
    for (AutoScalingGroup asg : autoScale.describeAutoScalingGroups(DescribeAutoScalingGroupsRequest.builder().maxRecords(100).build()).autoScalingGroups()) {
      String clusterId = getAsgTagValue("buildfarm.cluster_id", asg.tags());
      if (!clusterId.isEmpty() && !clusters.contains(clusterId)) {
        clusters.add(clusterId);
      }
    }
    return clusters;
  }

  @Override
  public List<ClusterInfo> getAllClustersWithDetails() {
    List<ClusterInfo> clusters = new ArrayList<>();
    for (String clusterId : getAllClusters()) {
      clusters.add(getClusterInfo(clusterId));
    }
    return clusters;
  }

  @Override
  public ClusterInfo getClusterInfo() {
    return getClusterInfo(clusterId);
  }

  @Override
  public ClusterInfo getClusterInfo(String clusterId) {
    ClusterInfo clusterInfo = new ClusterInfo();
    clusterInfo.setClusterId(clusterId);
    try {
      Asg serverAsg = new Asg();
      serverAsg.setGroupType("server");
      serverAsg.setAsg(getAutoScalingGroup(getAsgNamesFromHosts(clusterId, "server").get(0)));
      clusterInfo.setServers(serverAsg);
      List<Asg> workerAsgs = new ArrayList<>();
      for (String asgName : getAsgNamesFromHosts(clusterId, "worker")) {
        Asg workerAsg = new Asg();
        workerAsg.setGroupType("worker");
        workerAsg.setAsg(getAutoScalingGroup(asgName));
        workerAsg.setWorkerType(getAsgTagValue("buildfarm.worker_type", workerAsg.getAsg().tags()));
        workerAsgs.add(workerAsg);
      }
      clusterInfo.setWorkers(workerAsgs);
    } catch (Exception e) {
      logger.warn("Failed to retrieve cluster information from AWS: {}. Returning empty cluster info.", e.getMessage());
      // Return basic cluster info for demo mode
      clusterInfo.setServers(new Asg());
      clusterInfo.setWorkers(new ArrayList<>());
    }
    return clusterInfo;
  }

  @Override
  public ClusterDetails getClusterDetails() {
    ClusterDetails clusterDetails = new ClusterDetails();
    clusterDetails.setClusterId(clusterId);
    try {
      clusterDetails.setServers(getInstances(clusterId,"server", true));
      clusterDetails.setWorkers(getInstances(clusterId,"worker", true));
    } catch (Exception e) {
      logger.warn("Failed to retrieve cluster details from AWS: {}. Returning empty cluster details.", e.getMessage());
      clusterDetails.setServers(new ArrayList<>());
      clusterDetails.setWorkers(new ArrayList<>());
    }
    return clusterDetails;
  }

  @Override
  public int stopDockerContainer(String instanceId, String containerStr, String grpcEndpoint, int grpcPort) {
    ManagedChannel channel = ManagedChannelBuilder.forAddress(grpcEndpoint, grpcPort).usePlaintext().build();
    AdminGrpc.AdminBlockingStub stub = AdminGrpc.newBlockingStub(channel);
    StopContainerRequest request = StopContainerRequest.newBuilder().setHostId(instanceId).setContainerName(containerStr).build();
    Status status = stub.stopContainer(request);
    logger.info("Rebooted container {} using {}:{} with status {}", instanceId, grpcEndpoint, grpcPort, status);
    channel.shutdown();
    return status.getCode();
  }

  @Override
  public int terminateInstance(String instanceId, String grpcEndpoint, int grpcPort) {
    ManagedChannel channel = ManagedChannelBuilder.forAddress(grpcEndpoint, grpcPort).usePlaintext().build();
    AdminGrpc.AdminBlockingStub stub = AdminGrpc.newBlockingStub(channel);
    TerminateHostRequest request = TerminateHostRequest.newBuilder().setHostId(instanceId).build();
    Status status = stub.terminateHost(request);
    logger.info("Terminated instance {} using {}:{} with status {}", instanceId, grpcEndpoint, grpcPort, status);
    channel.shutdown();
    return status.getCode();
  }

  @Override
  public String getInstanceIdByPrivateDnsName(String dnsName) {
    Filter filter = Filter.builder().name("private-dns-name").values(dnsName).build();
    DescribeInstancesRequest describeInstancesRequest =
      DescribeInstancesRequest.builder().filters(filter).build();
    DescribeInstancesResponse instancesResult = ec2.describeInstances(describeInstancesRequest);
    for (Reservation r : instancesResult.reservations()) {
      for (software.amazon.awssdk.services.ec2.model.Instance e : r.instances()) {
        if (e.privateDnsName() != null && e.privateDnsName().equals(dnsName)) {
          return e.instanceId();
        }
      }
    }
    return null;
  }

  @Override
  public String scaleGroup(String asgName, Integer desiredInstances) {
    logger.info("Scaling group {} to {} instances", asgName, desiredInstances);
    UpdateAutoScalingGroupRequest request = UpdateAutoScalingGroupRequest.builder()
      .autoScalingGroupName(asgName).desiredCapacity(desiredInstances).build();
    UpdateAutoScalingGroupResponse response = autoScale.updateAutoScalingGroup(request);
    return response.toString();
  }

  @Override
  public void reindexCas(){
    ManagedChannel channel = ManagedChannelBuilder.forAddress(deploymentDomain, deploymentPort).usePlaintext().build();
    AdminGrpc.AdminFutureStub stub = AdminGrpc.newFutureStub(channel);
    ReindexCasRequest request = ReindexCasRequest.newBuilder().setInstanceName("shard").build();
    stub.reindexCas(request);
  }

  @Override
  public boolean isPrimaryAdminHost() {
    try {
      // For now, return true as primary host determination
      // TODO: Implement EC2 metadata client for v2 SDK
      return true;
    } catch (Exception e) {
      logger.warn("Could not determine if a primary host.", e);
    }
    return false;
  }

  private List<Instance> getInstances(String clusterId, String type, boolean getUptimes) {
    List<Instance> instances = new ArrayList<>();
    for (software.amazon.awssdk.services.ec2.model.Instance e : getEc2Instances(clusterId, type)) {
      Instance instance = new Instance();
      instance.setEc2Instance(e);
      instance.setClusterId(clusterId);
      if ("worker".equals(type)) {
        instance.setWorkerType(getTagValue("buildfarm.worker_type", e.tags()));
      }
      String clientKey= "startTime/" + e.privateIpAddress() + ("worker".equals(type) ? ":8981" : "");
      instance.setGroupType(type);
      instances.add(instance);
    }
    return getUptimes ? updateContainersUptimes(instances, type) : instances;
  }

  private List<Instance> updateContainersUptimes(List<Instance> instances, String type) {
    List<String> hostNames = new ArrayList<>();
    for (Instance instance : instances) {
      hostNames.add("startTime/" + instance.getEc2Instance().privateIpAddress() + ("worker".equals(type) ? ":8981" : ""));
    }
    Map<String, Long> allContainersUptime = getAllContainersUptime(hostNames);
    for (Instance instance : instances) {
      String clientKey = "startTime/" + instance.getEc2Instance().privateIpAddress() + ("worker".equals(type) ? ":8981" : "");
      instance.setContainerStartTime(allContainersUptime.get(clientKey) != null ? allContainersUptime.get(clientKey) : 0L );
    }
    return instances;
  }

  private Map<String, Long>  getAllContainersUptime(List<String> hostNames) {
    ManagedChannel channel = ManagedChannelBuilder.forAddress(deploymentDomain, deploymentPort).usePlaintext().build();
    AdminGrpc.AdminBlockingStub stub = AdminGrpc.newBlockingStub(channel);
    GetClientStartTimeRequest request = GetClientStartTimeRequest.newBuilder()
            .setInstanceName("shard")
            .addAllHostName(hostNames)
            .build();
    GetClientStartTimeResult result = stub.getClientStartTime(request);
    Map<String, Long> allContainersUptime = new HashMap<String, Long>();
    for (GetClientStartTime GetClientStartTime : result.getClientStartTimeList()){
      allContainersUptime.put(GetClientStartTime.getInstanceName(),GetClientStartTime.getClientStartTime().getSeconds());
    }
    if (channel != null) {
      channel.shutdown();
    }
    return allContainersUptime;
  }

  private AutoScalingGroup getAutoScalingGroup(String asgName) {
    DescribeAutoScalingGroupsRequest request = DescribeAutoScalingGroupsRequest.builder()
      .autoScalingGroupNames(Arrays.asList(asgName)).build();
    DescribeAutoScalingGroupsResponse response = autoScale.describeAutoScalingGroups(request);
    return response.autoScalingGroups().get(0);
  }

  private List<software.amazon.awssdk.services.ec2.model.Instance> getEc2Instances(String clusterId, String type) {
    List<software.amazon.awssdk.services.ec2.model.Instance> instances = new ArrayList<>();
    DescribeInstancesResponse instancesResult = ec2.describeInstances(
            DescribeInstancesRequest.builder().filters(
                    Filter.builder().name("instance-state-name").values("running").build(),
                    Filter.builder().name("tag:buildfarm.cluster_id").values(clusterId).build(),
                    Filter.builder().name("tag:buildfarm.instance_type").values(type).build()).build());
    for (Reservation r : instancesResult.reservations()) {
      for (software.amazon.awssdk.services.ec2.model.Instance e : r.instances()) {
        if (e != null) {
          instances.add(e);
        }
      }
    }
    return instances;
  }

  private List<String> getAsgNamesFromHosts(String clusterId, String type) {
    List<String> asgNames = new ArrayList<>();
    for (software.amazon.awssdk.services.ec2.model.Instance e : getEc2Instances(clusterId, type)) {
      for (Tag tag : e.tags()) {
        if ("aws:autoscaling:groupName".equalsIgnoreCase(tag.key()) && !asgNames.contains(tag.value())) {
          asgNames.add(tag.value());
        }
      }
    }
    return asgNames;
  }

  private String getTagValue(String tagName, List<Tag> tags) {
    for (Tag tag : tags) {
      if (tagName.equalsIgnoreCase(tag.key())) {
        return tag.value();
      }
    }
    return "";
  }

  private String getAsgTagValue(String tagName, List<TagDescription> tags) {
    for (TagDescription tag : tags) {
      if (tagName.equalsIgnoreCase(tag.key())) {
        return tag.value();
      }
    }
    return "";
  }
}
