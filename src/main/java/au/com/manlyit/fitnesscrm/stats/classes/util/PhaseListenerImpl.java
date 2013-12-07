/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

public class PhaseListenerImpl implements PhaseListener {

    @Override
    public void afterPhase(PhaseEvent event) {
        System.out.println("After Executing " + event.getPhaseId());

        PhaseId phaseId = event.getPhaseId();
        if (PhaseId.RESTORE_VIEW.equals(phaseId)) {
            // afterRestoreView();
        } else if (PhaseId.APPLY_REQUEST_VALUES.equals(phaseId)) {
            // afterApplyRequestValues();
        } else if (PhaseId.PROCESS_VALIDATIONS.equals(phaseId)) {
            // afterProcessValidations();
        } else if (PhaseId.UPDATE_MODEL_VALUES.equals(phaseId)) {
            // afterUpdateModelValues();
        } else if (PhaseId.INVOKE_APPLICATION.equals(phaseId)) {
            // afterInvokeApplication();
        } else if (PhaseId.RENDER_RESPONSE.equals(phaseId)) {
            if (!FacesContext.getCurrentInstance().getResponseComplete()) {
                // afterRenderResponse();
                //FacesContext context = FacesContext.getCurrentInstance();
                // TeChartDataController teChartDataController = (TeChartDataController) context.getApplication().evaluateExpressionGet(context, "#{teChartDataController}", TeChartDataController.class);
                // teChartDataController.redrawChartIfDirty();
                // teChartDataController.refreshChart();
            }
        }

    }

    @Override
    public void beforePhase(PhaseEvent event) {
        System.out.println("Before Executing " + event.getPhaseId());
        PhaseId phaseId = event.getPhaseId();
        if (PhaseId.RESTORE_VIEW.equals(phaseId)) {
            // beforeRestoreView();
        } else if (PhaseId.APPLY_REQUEST_VALUES.equals(phaseId)) {
            // beforeApplyRequestValues();
        } else if (PhaseId.PROCESS_VALIDATIONS.equals(phaseId)) {
            // beforeProcessValidations();
        } else if (PhaseId.UPDATE_MODEL_VALUES.equals(phaseId)) {
            //beforeUpdateModelValues();
        } else if (PhaseId.INVOKE_APPLICATION.equals(phaseId)) {
            // beforeInvokeApplication();
        } else if (PhaseId.RENDER_RESPONSE.equals(phaseId)) {
            if (!FacesContext.getCurrentInstance().getResponseComplete()) {
                //     beforeRenderResponse();
                FacesContext context = FacesContext.getCurrentInstance();
                // TeChartDataController teChartDataController = (TeChartDataController) context.getApplication().evaluateExpressionGet(context, "#{teChartDataController}", TeChartDataController.class);
                //  teChartDataController.redrawChartIfDirty();
            }
        }

    }

    @Override
    public PhaseId getPhaseId() {
        //return PhaseId.ANY_PHASE;
        return PhaseId.RENDER_RESPONSE;
    }
}
