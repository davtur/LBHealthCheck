# To change this license header, choose License Headers in Project Properties.
# To change this template file, choose Tools | Templates
# and open the template in the editor.
FROM payara/micro:latest
#ENV COPY_TMP /opt/dockersrc
#RUN mkdir -p $COPY_TMP
COPY ./FitnessCRM-5.4.6.war /opt/payara/deployments/
#COPY ./domain-fitnessCRM.xml $COPY_TMP


#RUN chown -R payara:payara $COPY_TMP
#RUN chmod -R 777 $COPY_TMP

#RUN mv /dockersrc/* /opt/payara/deployments/ &&\  
 #   chmod  777 /opt/payara/deployments/FitnessCRM-5.4.4.war /opt/payara/deployments/domain-fitnessCRM.xml

#CMD ["java","-jar","/opt/payara/payara-micro.jar","--deploymentDir","/opt/payara/deployments","--domainconfig","/opt/payara/deployments/domain-fitnessCRM.xml"]

CMD ["java","-jar","/opt/payara/payara-micro.jar","--deploymentDir","/opt/payara/deployments"]