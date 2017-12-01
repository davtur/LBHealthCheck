# To change this license header, choose License Headers in Project Properties.
# To change this template file, choose Tools | Templates
# and open the template in the editor.
FROM payara/micro:latest
#ENV COPY_TMP /opt/dockersrc
#RUN mkdir -p $COPY_TMP

#COPY ./FitnessCRM-6.1.0.war /opt/payara/deployments/
COPY /target/FitnessCRM-6.1.0.war /opt/payara/deployments/
#COPY ./domain-fitnessCRM.xml $COPY_TMP
COPY /domain-fitnessCRM.xml /opt/payara/

USER root
RUN chown -R payara:payara /opt/payara/domain-fitnessCRM.xml
RUN chmod -R 664 /opt/payara/domain-fitnessCRM.xml
RUN chown -R payara:payara /opt/payara/deployments/FitnessCRM-6.1.0.war
RUN chmod -R 664 /opt/payara/deployments/FitnessCRM-6.1.0.war

USER payara
#RUN mv /dockersrc/* /opt/payara/deployments/ &&\  
 #   chmod  777 /opt/payara/deployments/FitnessCRM-5.4.4.war /opt/payara/deployments/domain-fitnessCRM.xml

CMD ["java","-jar","/opt/payara/payara-micro.jar","--deploymentDir","/opt/payara/deployments","--domainconfig","/opt/payara/domain-fitnessCRM.xml"]

#CMD ["java","-jar","/opt/payara/payara-micro.jar","--deploymentDir","/opt/payara/deployments"]