FROM jboss/wildfly:12.0.0.Final


ADD standalone-custom.xml /opt/jboss/wildfly/standalone/configuration/
ADD home-manlyit.p12      /opt/jboss/wildfly/standalone/configuration/

ADD module.xml                      /opt/jboss/wildfly/modules/system/layers/base/org/eclipse/persistence/main
ADD eclipselink-2.7.1.jar           /opt/jboss/wildfly/modules/system/layers/base/org/eclipse/persistence/main

ADD mysql-connector-java-5.1.45.jar /opt/jboss/wildfly/standalone/deployments/

ADD FitnessCRM-*.war                /opt/jboss/wildfly/standalone/deployments/

RUN /opt/jboss/wildfly/bin/add-user.sh admin Admin#70365 --silent
EXPOSE 8080 8081 9990 9993
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-c", "standalone-custom.xml", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]
