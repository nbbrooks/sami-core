package sami.engine;

import sami.mission.MissionPlanSpecification;
import sami.mission.Place;

/**
 *
 * @author pscerri
 */
public interface PlanManagerListenerInt {

    public void planCreated(PlanManager planManager, MissionPlanSpecification mSpec);

    public void planStarted(PlanManager planManager);

    public void planEnteredPlace(PlanManager planManager, Place place);

    public void planLeftPlace(PlanManager planManager, Place place);
    
    public void planRepaint(PlanManager planManager);

    public void planFinished(PlanManager planManager);

    public void planAborted(PlanManager planManager);
}
