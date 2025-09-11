package tech.aurora.bfadmin.service.impl;

import tech.aurora.bfadmin.service.AdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AdminServiceImpl implements AdminService {
  private static final Logger logger = LoggerFactory.getLogger(AdminServiceImpl.class);

  @Override
  public String getHelloMessage() {
    logger.info("Hello message requested");
    return "Hello World from Buildfarm Admin!";
  }
}
