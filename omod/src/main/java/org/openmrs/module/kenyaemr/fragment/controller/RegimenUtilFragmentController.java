/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.fragment.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.*;
import org.openmrs.api.*;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.Dictionary;
import org.openmrs.module.kenyaemr.EmrWebConstants;
import org.openmrs.module.kenyaemr.regimen.*;
import org.openmrs.module.kenyaui.KenyaUiUtils;
import org.openmrs.module.kenyaui.form.ValidatingCommandObject;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.BindParams;
import org.openmrs.ui.framework.annotation.MethodParam;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.openmrs.util.OpenmrsConstants;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Various actions for regimen related functions
 */
public class RegimenUtilFragmentController {

	protected static final Log log = LogFactory.getLog(RegimenUtilFragmentController.class);

	/**
	 * Changes the patient's current regimen
	 * @param command the command object
	 * @param ui the UI utils
	 * @return the patient's current regimen
	 */
	public void changeRegimen(@MethodParam("newRegimenChangeCommandObject") @BindParams RegimenChangeCommandObject command, UiUtils ui) {

		ui.validate(command, command, null);
		command.apply();
	}

	public void createRegimenEventEncounter(@MethodParam("newRegimenChangeCommandObject") @BindParams RegimenChangeCommandObject command, UiUtils ui) {
		ui.validate(command, command, null);

		ConceptService cs = Context.getConceptService();
		EncounterService encounterService = Context.getEncounterService();
		Encounter encounter = new Encounter();
		Date date = new Date();
		EncounterType encounterType = encounterService.getEncounterTypeByUuid("465a92f2-baf8-42e9-9612-53064be868e8");
		encounter.setPatient(command.getPatient());
		encounter.setEncounterType(encounterType);
		encounter.setEncounterDatetime(command.getChangeDate());
		encounter.setDateCreated(date);
		Concept con = cs.getConceptByUuid(command.getRegimenDetails());

		//create an obs for regimen
		Obs o = new Obs();
		o.setConcept(cs.getConcept(1193));
		o.setDateCreated(new Date());
		o.setCreator(Context.getAuthenticatedUser());
		// o.setLocation(new Location(1));
		o.setObsDatetime(command.getChangeDate());
		o.setPerson(command.getPatient());
		o.setValueCoded(con);
		encounter.addObs(o);

		//create  obs for Change reason coded
		if(command.getChangeReason() !=null) {
			Obs o2 = new Obs();
			o2.setConcept(cs.getConcept(1252));
			o2.setDateCreated(new Date());
			o2.setCreator(Context.getAuthenticatedUser());
			// o2.setLocation(new Location(1));
			o2.setObsDatetime(command.getChangeDate());
			o2.setValueCoded(command.getChangeReason());
			o2.setPerson(command.getPatient());
			encounter.addObs(o2);
		}
		//create  obs for Change reason Noncoded
		if (!command.getChangeReasonNonCoded().isEmpty()) {
			Obs o3 = new Obs();
			o3.setConcept(cs.getConcept(5622));
			o3.setDateCreated(new Date());
			o3.setCreator(Context.getAuthenticatedUser());
			// o3.setLocation(new Location(1));
			o3.setObsDatetime(command.getChangeDate());
			o3.setValueText(command.getChangeReasonNonCoded());
			o3.setPerson(command.getPatient());
			encounter.addObs(o3);
		}

		// create obs for plan TB/ARV 1268/1255

		Obs category = new Obs();
		category.setDateCreated(new Date());
		category.setCreator(Context.getAuthenticatedUser());
		// category.setLocation(new Location(1));
		category.setObsDatetime(command.getChangeDate());
		category.setPerson(command.getPatient());
		if(command.getChangeType()==RegimenChangeType.CHANGE) {
			category.setValueCoded(cs.getConcept(1259));
		}

		if(command.getChangeType()==RegimenChangeType.STOP) {
			category.setValueCoded(cs.getConcept(1260));
		}

		if(command.getChangeType()==RegimenChangeType.START) {
			category.setValueCoded(cs.getConcept(1256));
		}

		if(command.getCategory().equalsIgnoreCase("ARV") ) {
			category.setConcept(cs.getConcept(1255));

		}else if (command.getCategory().equalsIgnoreCase("TB")) {
			category.setConcept(cs.getConcept(1268));

		}
		encounter.addObs(category);


		// System.out.println("this method has been called========================*****************"+encounter);
		encounterService.saveEncounter(encounter);

	}

	/**
	 * Undoes the last regimen change for the given patient
	 * @param patient the patient
	 * @return the patient's current regimen
	 */
	public void undoLastChange(@RequestParam("patient") Patient patient, HttpSession session, @RequestParam("category") String category, @SpringBean RegimenManager regimenManager, @SpringBean KenyaUiUtils kenyaUi) {
		Concept masterSet = regimenManager.getMasterSetConcept(category);
		RegimenChangeHistory history = RegimenChangeHistory.forPatient(patient, masterSet);
		history.undoLastChange();

		kenyaUi.notifySuccess(session, "Removed last regimen change");
	}

	/**
	 * Helper method to create a new form object
	 * @return the form object
	 */
	public RegimenChangeCommandObject newRegimenChangeCommandObject(@SpringBean RegimenManager regimenManager) {
		return new RegimenChangeCommandObject(regimenManager);
	}

	/**
	 * Change types
	 */
	public enum RegimenChangeType {
		START,
		CHANGE,
		STOP,
		RESTART
	}

	/**
	 * Command object for regimen changes
	 */
	public class RegimenChangeCommandObject extends ValidatingCommandObject {

		private RegimenManager regimenManager;

		private Patient patient;

		private String category;

		private RegimenChangeType changeType;

		private Date changeDate;

		private Concept changeReason;

		private String changeReasonNonCoded;

		private Regimen regimen;

		private String regimenDetails;

		public RegimenChangeCommandObject(RegimenManager regimenManager) {
			this.regimenManager = regimenManager;
		}

		/**
		 * @see org.springframework.validation.Validator#validate(java.lang.Object,org.springframework.validation.Errors)
		 */
		@Override
		public void validate(Object target, Errors errors) {
			System.out.println("otherNonCoded==================target"+target);

			require(errors, "patient");
			require(errors, "category");
			require(errors, "changeType");
			require(errors, "changeDate");

			// Reason is only required for stopping or changing
			if (changeType == RegimenChangeType.STOP || changeType == RegimenChangeType.CHANGE) {
				require(errors, "changeReason");

				if (changeReason != null) {
					Concept otherNonCoded = Dictionary.getConcept(Dictionary.OTHER_NON_CODED);


					if (changeReason.equals(otherNonCoded)) {
						require(errors, "changeReasonNonCoded");
					}
				}
			}

			if (category != null && changeDate != null) {
				// Get patient regimen history
				Concept masterSet = regimenManager.getMasterSetConcept(category);
				RegimenChangeHistory history = RegimenChangeHistory.forPatient(patient, masterSet);
				RegimenChange lastChange = history.getLastChange();
				boolean onRegimen = lastChange != null && lastChange.getStarted() != null;

				// Can't start if already started
				/*if ((changeType == RegimenChangeType.START || changeType == RegimenChangeType.RESTART) && onRegimen) {
					errors.reject("Can't start regimen for patient who is already on a regimen");
				}

				// Changes must be in order
				if (lastChange != null && OpenmrsUtil.compare(changeDate, lastChange.getDate()) <= 0) {
					errors.rejectValue("changeDate", "Change date must be after all other changes");
				}*/

				// Don't allow future dates
				if (OpenmrsUtil.compare(changeDate, new Date()) > 0) {
					errors.rejectValue("changeDate", "Change date can't be in the future");
				}
			}

			// Validate the regimen
			/*if (changeType != RegimenChangeType.STOP) {
				try {
					errors.pushNestedPath("regimen");
					ValidationUtils.invokeValidator(new RegimenValidator(), regimen, errors);
				} finally {
					errors.popNestedPath();
				}
			}*/
		}

		/**
		 * Applies this regimen change
		 */
		public void apply() {
			Concept masterSet = regimenManager.getMasterSetConcept(category);
			RegimenChangeHistory history = RegimenChangeHistory.forPatient(patient, masterSet);
			RegimenChange lastChange = history.getLastChange();
			RegimenOrder baseline = lastChange != null ? lastChange.getStarted() : null;

			if (baseline == null) {
				for (RegimenComponent component : regimen.getComponents()) {
					DrugOrder o = component.toDrugOrder(patient, changeDate);
					Context.getOrderService().saveOrder(o, null);
				}
			}
			else {
				List<DrugOrder> noChanges = new ArrayList<DrugOrder>();
				List<DrugOrder> toChangeDose = new ArrayList<DrugOrder>();
				List<DrugOrder> toStart = new ArrayList<DrugOrder>();

				if (regimen != null) {
					for (RegimenComponent component : regimen.getComponents()) {
						changeRegimenHelper(baseline, component, noChanges, toChangeDose, toStart);
					}
				}

				List<DrugOrder> toStop = new ArrayList<DrugOrder>(baseline.getDrugOrders());
				// for now "toChangeDose" is handled the same as toStop
				// toStop.removeAll(toChangeDose);
				toStop.removeAll(noChanges);

				OrderService os = Context.getOrderService();

				for (DrugOrder o : toStop) {
					//o.setDiscontinued(true);
					o.setDateChanged(changeDate);
					o.setAction(Order.Action.DISCONTINUE);
					//o.setDiscontinuedBy(Context.getAuthenticatedUser());
					//o.setDiscontinuedReason(changeReason);
					//o.setDiscontinuedReasonNonCoded(changeReasonNonCoded);
					os.saveOrder(o, null);
				}

				for (DrugOrder o : toStart) {
					o.setPatient(patient);
					o.setDateActivated(changeDate);
					o.setOrderType(os.getOrderType(OpenmrsConstants.ORDERTYPE_DRUG));
					os.saveOrder(o, null);
				}
			}
		}

		/**
		 * Gets the patient
		 * @return the patient
		 */
		public Patient getPatient() {
			return patient;
		}

		/**
		 * Sets the patient
		 * @param patient the patient
		 */
		public void setPatient(Patient patient) {
			this.patient = patient;
		}

		/**
		 * Gets the regimen category
		 * @return the regimen category
		 */
		public String getCategory() {
			return category;
		}

		/**
		 * Sets the regimen category
		 * @param category the category
		 */
		public void setCategory(String category) {
			this.category = category;
		}

		/**
		 * Gets the change type
		 * @return the change type
		 */
		public RegimenChangeType getChangeType() {
			return changeType;
		}

		/**
		 * Sets the change type
		 * @param changeType the change type
		 */
		public void setChangeType(RegimenChangeType changeType) {
			this.changeType = changeType;
		}

		/**
		 * Gets the change date
		 * @return the change date
		 */
		public Date getChangeDate() {
			return changeDate;
		}

		/**
		 * Set the change date
		 * @param changeDate the change date
		 */
		public void setChangeDate(Date changeDate) {
			this.changeDate = changeDate;
		}

		/**
		 * Gets the change reason
		 * @return the change reason
		 */
		public Concept getChangeReason() {
			return changeReason;
		}

		/**
		 * Sets the change reason
		 * @param changeReason the change reason
		 */
		public void setChangeReason(Concept changeReason) {
			this.changeReason = changeReason;
		}

		/**
		 * Gets the non-coded change reason
		 * @return the non-coded change reason
		 */
		public String getChangeReasonNonCoded() {
			return changeReasonNonCoded;
		}

		/**
		 * Sets the non-coded change reason
		 * @param changeReasonNonCoded the non-coded change reason
		 */
		public void setChangeReasonNonCoded(String changeReasonNonCoded) {
			this.changeReasonNonCoded = changeReasonNonCoded;
		}

		/**
		 * Gets the regimen
		 * @return the regimen
		 */
		public Regimen getRegimen() {
			return regimen;
		}

		/**
		 * Sets the regimen
		 * @param regimen the regimen
		 */
		public void setRegimen(Regimen regimen) {
			this.regimen = regimen;
		}


		public String getRegimenDetails() {
			return regimenDetails;
		}

		/**
		 * Sets the regimenDetails
		 * @param regimenDetails the regimenDetails
		 */
		public void setRegimenDetails(String regimenDetails) {
			this.regimenDetails = regimenDetails;
		}


	}

	/**
	 * Analyzes the current regimen order and the new regimen component to decide which orders must be changed
	 * @param baseline the current regimen order
	 * @param component the new regimen component
	 * @param noChanges
	 * @param toChangeDose
	 * @param toStart
	 */
	private void changeRegimenHelper(RegimenOrder baseline, RegimenComponent component, List<DrugOrder> noChanges, List<DrugOrder> toChangeDose,
									 List<DrugOrder> toStart) {

		List<DrugOrder> sameGeneric = baseline.getDrugOrders(component.getDrugRef());

		boolean anyDoseChanges = false;
		for (DrugOrder o : sameGeneric) {
			//TODO relook at this
			if (o.getDose().equals(component.getDose()) && o.getQuantityUnits().equals(component.getUnits()) && OpenmrsUtil.nullSafeEquals(o.getFrequency().getConcept(), component.getFrequency())) {
				noChanges.add(o);
			} else {
				toChangeDose.add(o);
				anyDoseChanges = true;
			}
		}
		if (anyDoseChanges || sameGeneric.size() == 0) {
			toStart.add(component.toDrugOrder(null, null));
		}
	}
}