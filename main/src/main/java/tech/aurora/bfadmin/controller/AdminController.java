package tech.aurora.bfadmin.controller;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import tech.aurora.bfadmin.config.SecurityConfig;
import tech.aurora.bfadmin.model.ClusterInfo;
import tech.aurora.bfadmin.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.PostConstruct;
import java.util.UUID;

@Controller
public class AdminController {
  private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

  @Autowired
  AdminService adminService;

  @Value("${buildfarm.cluster.name}")
  private String clusterId;

  @Value("${deployment.domain}")
  private String deploymentDomain;

  @Value("${buildfarm.public.port}")
  private int deploymentPort;

  @Value("${aws.region}")
  private String region;

  @Value("${ui.enable}")
  private boolean ui;

  private AmazonEC2 ec2;
  private ClusterInfo clusterInfo;

  @RequestMapping("/")
  public String getMainApp() {
    return "redirect:/dashboard";
  }

  @RequestMapping("/dashboard")
  public String getDashboard(Model model) {
    if (ui) {
      generateSecurityKey();
      model.addAttribute("clusterInfo", clusterInfo != null ? clusterInfo : new ClusterInfo());
      try {
        model.addAttribute("clusterDetails", adminService.getClusterDetails());
      } catch (Exception e) {
        logger.warn("Failed to get cluster details for dashboard: {}", e.getMessage());
        model.addAttribute("clusterDetails", null);
      }
      model.addAttribute("awsRegion", region);
      model.addAttribute("securityKey", SecurityConfig.securityKey);
      return "dashboard";
    } else {
      model.addAttribute("status", "999");
      model.addAttribute("error", "Not Enabled");
      model.addAttribute("message", "UI is not enabled. Set ui.enable=true in application.properties");
      return "error";
    }
  }

  @PostConstruct
  public void init() {
    logger.info("Initializing aws sdk for region {}", region);
    try {
      ec2 = AmazonEC2ClientBuilder.standard().withRegion(region).build();
      clusterInfo = adminService.getClusterInfo();
      generateSecurityKey();
      logger.info("Found Buildfarm deployment in AWS account: clusterInfo [ number of servers: {}, number of worker groups: {}, grpc endpoint: {}:{}",
              clusterInfo.getServers().getAsg().getInstances().size(), clusterInfo.getWorkers().size(), deploymentDomain, deploymentPort);
    } catch (Exception e) {
      logger.warn("Failed to initialize AWS connection: {}. Application will start without AWS integration.", e.getMessage());
      generateSecurityKey();
      // Initialize empty cluster info for demo mode
      clusterInfo = new ClusterInfo();
      clusterInfo.setClusterId("demo-cluster");
    }
  }

  public String getBaseClusterId() {
    return clusterId.replace("buildfarm-", "");
  }

  /**
   * Generate a new security key valid for a single request from UI
   */
  private void generateSecurityKey() {
    SecurityConfig.securityKey = UUID.randomUUID().toString();
  }
}
