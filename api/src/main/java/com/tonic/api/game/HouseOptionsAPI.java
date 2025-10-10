package com.tonic.api.game;

import net.runelite.api.gameval.VarbitID;

public class HouseOptionsAPI
{

  //TODO setters

  /**
   * @return true if the default building mode state is toggled
   */
  public static boolean isDefaultBuildingMode()
  {
    return VarAPI.getVar(VarbitID.POH_TELEPORT_BUILDING_MODE) == 1;
  }

  /**
   * @return true if the default teleport option for POH is set to inside
   */
  public static boolean isDefaultTeleportInside()
  {
    return VarAPI.getVar(VarbitID.POH_TELE_TOGGLE) == 0;
  }

  /**
   * @return The current {@link DoorRenderMode} of your POH
   */
  public static DoorRenderMode getDoorRenderMode()
  {
    DoorRenderMode[] modes = DoorRenderMode.values();
    int state = VarAPI.getVar(VarbitID.POH_DOORS_OPTION);
    if (state >= 0 && state < modes.length)
    {
      return modes[state];
    }

    return DoorRenderMode.UNDETERMINED;
  }

  enum DoorRenderMode
  {
    CLOSED,
    OPEN,
    HIDDEN,
    UNDETERMINED
  }
}
