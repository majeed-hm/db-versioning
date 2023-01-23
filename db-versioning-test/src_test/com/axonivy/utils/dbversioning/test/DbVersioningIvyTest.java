package com.axonivy.utils.dbversioning.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.environment.IvyTest;

/**
 */
@IvyTest
public class DbVersioningIvyTest{
  
  @Test
  public void useIvy(){
    Ivy.log().info("hi from JUnit");
    assertThat(true).as("I can use Ivy API facade in tests").isEqualTo(true);
  }
  
}
