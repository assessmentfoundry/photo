module phm.photo {
  requires com.drewnoakes.metadataextractor;
  requires org.slf4j;
  requires org.apache.logging.log4j;
  requires org.apache.logging.log4j.slf4j;
  requires jcommander;
  requires static lombok;
//  requires org.testng;

  exports photo;
}