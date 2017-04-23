# To change this license header, choose License Headers in Project Properties.
# To change this template file, choose Tools | Templates
# and open the template in the editor.
FROM payara/micro:latest

COPY ./FitnessCRM-5.4.4.war /opt/payara/deployments
COPY ./domain-fitnessCRM.xml /opt/payara/deployments
RUN chown -R payara:payara /opt
CMD ["java","-jar","payara-micro.jar --deploymentDir /opt/payara/deployments --domainConfig /opt/payara/deployments/domain-fitnessCRM.xml"]

