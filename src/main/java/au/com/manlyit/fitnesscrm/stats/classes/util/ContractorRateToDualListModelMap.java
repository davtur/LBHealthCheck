/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import au.com.manlyit.fitnesscrm.stats.db.ContractorRates;
import au.com.manlyit.fitnesscrm.stats.db.SessionTypes;
import org.primefaces.model.DualListModel;

/**
 *
 * @author david
 */
public class ContractorRateToDualListModelMap {

    private DualListModel<SessionTypes> dualList;
    private ContractorRates contractorRates;

    
    public ContractorRateToDualListModelMap(DualListModel<SessionTypes> dualList,ContractorRates contractorRates){
        this.contractorRates = contractorRates;
        this.dualList = dualList;
    }
    
   
    
    /**
     * @return the dualList
     */
    public DualListModel<SessionTypes> getDualList() {
        return dualList;
    }

    /**
     * @param dualList the dualList to set
     */
    public void setDualList(DualListModel<SessionTypes> dualList) {
        this.dualList = dualList;
    }

    /**
     * @return the contractorRates
     */
    public ContractorRates getContractorRates() {
        return contractorRates;
    }

    /**
     * @param contractorRates the contractorRates to set
     */
    public void setContractorRates(ContractorRates contractorRates) {
        this.contractorRates = contractorRates;
    }

}
