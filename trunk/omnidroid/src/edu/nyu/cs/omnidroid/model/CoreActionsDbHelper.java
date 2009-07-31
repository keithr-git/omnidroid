/*******************************************************************************
 * Copyright 2009 OmniDroid - http://code.google.com/p/omnidroid
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package edu.nyu.cs.omnidroid.model;

import static edu.nyu.cs.omnidroid.model.CursorHelper.getStringFromCursor;
import static edu.nyu.cs.omnidroid.model.CursorHelper.getLongFromCursor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import edu.nyu.cs.omnidroid.core.Action;
import edu.nyu.cs.omnidroid.core.CallPhoneAction;
import edu.nyu.cs.omnidroid.core.Event;
import edu.nyu.cs.omnidroid.core.SendSmsAction;
import edu.nyu.cs.omnidroid.model.db.DbHelper;
import edu.nyu.cs.omnidroid.model.db.RegisteredActionDbAdapter;
import edu.nyu.cs.omnidroid.model.db.RegisteredActionParameterDbAdapter;
import edu.nyu.cs.omnidroid.model.db.RuleActionDbAdapter;
import edu.nyu.cs.omnidroid.model.db.RuleActionParameterDbAdapter;
import edu.nyu.cs.omnidroid.model.db.RuleDbAdapter;
import edu.nyu.cs.omnidroid.util.ExceptionMessageMap;
import edu.nyu.cs.omnidroid.util.OmLogger;
import edu.nyu.cs.omnidroid.util.OmnidroidException;

/**
 * This class serves as a database access layer for Omnidroid's Actions framework.
 * 
 */
public class CoreActionsDbHelper {

  private DbHelper omnidroidDbHelper;
  private SQLiteDatabase database;
  private RuleDbAdapter ruleDbAdapter;
  private RuleActionDbAdapter ruleActionDbAdpater;
  private RuleActionParameterDbAdapter ruleActionParameterDbAdapter;
  private RegisteredActionDbAdapter registeredActionDbAdapter;
  private RegisteredActionParameterDbAdapter registeredActionParameterDbAdapter;
  private Context context;

  // This flag marks whether this helper is closed
  private boolean isClosed = false;

  public CoreActionsDbHelper(Context context) {
    omnidroidDbHelper = new DbHelper(context);
    database = omnidroidDbHelper.getReadableDatabase();
    this.context = context;

    // Initialize db adapters
    ruleDbAdapter = new RuleDbAdapter(database);
    ruleActionDbAdpater = new RuleActionDbAdapter(database);
    ruleActionParameterDbAdapter = new RuleActionParameterDbAdapter(database);
    registeredActionDbAdapter = new RegisteredActionDbAdapter(database);
    registeredActionParameterDbAdapter = new RegisteredActionParameterDbAdapter(database);
  }

  /**
   * Close the CoreActionsDBHelper. The core classes need to call this method when they are done
   * with the database connection. CoreActionDbHelper is not usable after calling this method.
   */
  public void close() {
    isClosed = true;
    omnidroidDbHelper.close();
    database.close();
  }

  /**
   * This method initializes an Action object from a given action name and parameters
   * 
   * @param actionName
   *          Name of the action
   * @param actionParams
   *          Parameters required to initialize the action
   * @return An action object
   * @throws OmnidroidException
   *           if the given action cannot be initialized by this method
   */
  private Action getAction(String actionName, HashMap<String, String> actionParams)
      throws OmnidroidException {
    // TODO:(Rutvij) Fetch this hard coded data from database.
    if (actionName == "SMS Send") {
      return new SendSmsAction(actionParams);
    } else if (actionName == "Phone Call") {
      return new CallPhoneAction(actionParams);
    } else
      throw new OmnidroidException(120003, ExceptionMessageMap.getMessage(new Integer(120003)
          .toString()));
  }

  /**
   * This method checks the parameter data to see if it already has value or it should extract value
   * from the event.
   * 
   * @param paramData
   *          The parameter data
   * @param event
   *          The event whose attributes can be used as parameter data
   * @return The parameter data with actual value that can be used in intent
   * @throws IllegalArgumentException
   *           if the attribute name is not valid for the event
   * @throws IllegalStateException
   *           if the helper is closed
   */
  private String extractData(String paramData, Event event) throws IllegalArgumentException {
    if (isClosed) {
      throw new IllegalStateException("CoreActionsDBHelper is closed.");
    }

    // Parameter data to be extracted from an event's attributes has the format "<attribute_name>"
    Pattern pattern = Pattern.compile("^<(.*)>$");
    Matcher matcher = pattern.matcher(paramData);
    final int EVENT_ATTRIB = 1; // Name of the event attribute in the pattern.
    String eventAttr;
    if (matcher.find()) {
      eventAttr = matcher.group(EVENT_ATTRIB);
      return event.getAttribute(eventAttr); // return value of the event attribute
    } else {
      return paramData; // return the value in parameter data
    }
  }

  /**
   * This method returns an ArrayList of action ids which are to be executed for a given rule
   * 
   * @param ruleId
   *          The rule Id
   * @return ArrayList of Action Ids for actions to be executed
   * @throws IllegalStateException
   *           if the helper is closed
   */
  private ArrayList<Long> getRuleActionIds(Long ruleId) {
    if (isClosed) {
      throw new IllegalStateException("CoreActionsDBHelper is closed.");
    }

    Cursor cursor = ruleActionDbAdpater.fetchAll(ruleId, null);
    ArrayList<Long> ruleActionIds = new ArrayList<Long>();
    for (int i = 0; i < cursor.getCount(); i++) {
      cursor.moveToNext();
      ruleActionIds.add(getLongFromCursor(cursor, RuleActionDbAdapter.KEY_RULEACTIONID));
    }
    cursor.close();
    return ruleActionIds;
  }

  /**
   * This method gives the registered action name for a rule action
   * 
   * @param ruleActionId
   *          The Rule Action's Id
   * @return Registered Action name for the rule action. Null if cannot find actionName or actionId
   * @throws IllegalStateException
   *           if the helper is closed
   */
  private String getRegisteredActionName(Long ruleActionId) {
    if (isClosed) {
      throw new IllegalStateException("CoreActionsDBHelper is closed.");
    }

    Long actionId;
    String actionName;
    Cursor cursor = ruleActionDbAdpater.fetch(ruleActionId);
    if (cursor.moveToFirst()) {
      actionId = getLongFromCursor(cursor, RuleActionDbAdapter.KEY_ACTIONID);
      cursor.close();
    } else {
      return null;
    }

    cursor = registeredActionDbAdapter.fetch(actionId);
    if (cursor.moveToFirst()) {
      actionName = getStringFromCursor(cursor, RegisteredActionDbAdapter.KEY_ACTIONNAME);
      cursor.close();
    } else {
      return null;
    }

    return actionName;
  }

  /**
   * This method gets Parameter Id, Parameter Data, and Registered Parameter Name for all parameters
   * of a rule action from the database and adds them to two the two maps paramsData and
   * paramsRegisteredParamId.
   * 
   * @param ruleActionId
   *          Rule Action's Id
   * @param event
   *          Event from which data is extracted
   * @param paramsData
   *          An empty map of Rule action parameter Id to Rule action parameter Data
   * @param paramsRegisteredParamId
   *          An empty map of Action parameter Id to Registered action parameter Id
   * @throws IllegalStateException
   *           if the helper is closed
   */
  private void addDataAndRegisteredParamId(Long ruleActionId, Event event,
      HashMap<Long, String> paramsData, HashMap<Long, Long> paramsRegisteredParamId) {
    if (isClosed) {
      throw new IllegalStateException("CoreActionsDBHelper is closed.");
    }

    Long paramId;
    String paramData;
    Long paramRegisteredParamId;
    Cursor cursor = ruleActionParameterDbAdapter.fetchAll(ruleActionId, null, null);
    for (int i = 0; i < cursor.getCount(); i++) {
      cursor.moveToNext();
      paramId = getLongFromCursor(cursor, RuleActionParameterDbAdapter.KEY_RULEACTIONPARAMETERID);
      paramData = getStringFromCursor(cursor,
          RuleActionParameterDbAdapter.KEY_RULEACTIONPARAMETERDATA);
      paramRegisteredParamId = getLongFromCursor(cursor,
          RuleActionParameterDbAdapter.KEY_ACTIONPARAMETERID);
      try {
        paramsData.put(paramId, extractData(paramData, event));
      } catch (IllegalArgumentException e) {
        Log.w(this.getClass().getName() + ": ", e.toString(), e);
        Log.w(this.getClass().getName() + ": ", e.getLocalizedMessage());
        OmLogger.write(context, "Attribute " + paramData + " is not valid for the event");
      }
      paramsRegisteredParamId.put(paramId, paramRegisteredParamId);
    }
    cursor.close();
  }

  /**
   * This method gets a map of all Registered Action Parameter ids and names and returns a map of
   * Ids to Names for lookup.
   * 
   * @return HashMap of Registered action parameter Ids to Registered param names for all registered
   *         action parameters
   * @throws IllegalStateException
   *           if the helper is closed
   */
  private HashMap<Long, String> getRegisteredActionParamNames() {
    if (isClosed) {
      throw new IllegalStateException("CoreActionsDBHelper is closed.");
    }

    Long registeredParamId;
    String registeredParamName;
    HashMap<Long, String> registeredParamNames = new HashMap<Long, String>();
    Cursor cursor = registeredActionParameterDbAdapter.fetchAll();
    for (int i = 0; i < cursor.getCount(); i++) {
      cursor.moveToNext();
      registeredParamId = getLongFromCursor(cursor,
          RegisteredActionParameterDbAdapter.KEY_ACTIONPARAMETERID);
      registeredParamName = getStringFromCursor(cursor,
          RegisteredActionParameterDbAdapter.KEY_ACTIONPARAMETERNAME);
      registeredParamNames.put(registeredParamId, registeredParamName);
    }
    cursor.close();
    return registeredParamNames;
  }

  /**
   * This method gives an ArrayList of actions to be executed for a given rule. Populates the action
   * parameter fields, which may require retrieving them from the event
   * 
   * @param ruleId
   *          Id of the rule
   * @param event
   *          Event that triggered this rule
   * @return ArrayList of actions to be executed
   * @throws IllegalArgumentException
   *           if the rule is not enabled or not found in database OR if ActionId or ActionName is
   *           not found in database
   * @throws IllegalStateException
   *           if the helper is closed
   */
  public ArrayList<Action> getActions(long ruleId, Event event) {
    if (isClosed) {
      throw new IllegalStateException("CoreActionsDBHelper is closed.");
    }

    ArrayList<Action> actions = new ArrayList<Action>();

    // Get list of Rule Action Ids from database
    ArrayList<Long> ruleActionIds = getRuleActionIds(ruleId);

    // Create an action object for each rule action
    Action action;
    String actionName;
    HashMap<Long, String> paramsData; // <ruleActionParamId, ruleActionParamData>
    HashMap<Long, Long> paramsRegisteredParamId; // <ruleActionParamId, registeredActionParamId>
    for (Long ruleActionId : ruleActionIds) {
      // get action name
      actionName = getRegisteredActionName(ruleActionId);
      if (actionName == null) {
        throw new IllegalArgumentException("Cannot find ActionId or ActionName for: "
            + ruleActionId);
      }
      // get parameter ids and data for the current action
      paramsData = new HashMap<Long, String>();
      paramsRegisteredParamId = new HashMap<Long, Long>();
      addDataAndRegisteredParamId(ruleActionId, event, paramsData, paramsRegisteredParamId);

      // Get registered action parameter names
      // <registeredActionParamId, registeredActionParamName>
      HashMap<Long, String> registeredParamNames = getRegisteredActionParamNames();

      // Create a map of parameter name to parameter data
      // <registeredActionParamName, ruleActionParamData>
      HashMap<String, String> actionParams = new HashMap<String, String>();
      for (Long parameterId : paramsData.keySet()) {
        actionParams.put(registeredParamNames.get(paramsRegisteredParamId.get(parameterId)),
            paramsData.get(parameterId));
      }

      // create action using action parameters and action name
      try {
        action = getAction(actionName, actionParams);
        actions.add(action); // add action to actions ArrayList
      } catch (OmnidroidException e) {
        Log.w(this.getClass().getName() + ": ", e.toString(), e);
        Log.w(this.getClass().getName() + ": ", e.getLocalizedMessage());
        OmLogger.write(context, "Action " + actionName + " cannot be initialized");
      }
    }
    return actions;
  }
}
