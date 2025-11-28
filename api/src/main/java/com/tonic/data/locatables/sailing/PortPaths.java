/*
 * Copyright (c) 2025, nucleon <https://github.com/nucleon>
 * Copyright (c) 2025, Cooper Morris <https://github.com/coopermor>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tonic.data.locatables.sailing;

import com.tonic.services.pathfinder.sailing.BoatPathing;
import com.tonic.util.Profiler;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public enum PortPaths {
    DEFAULT(
            PortLocation.EMPTY,
            PortLocation.EMPTY
            // Sailing >= 0, used in 0 tasks
    ),
    CATHERBY_BRIMHAVEN(
            PortLocation.CATHERBY,
            PortLocation.BRIMHAVEN,
            // Sailing >= 25, used in 4 tasks
            new RelativeMove(0, -22),
            new RelativeMove(-42, -42)
    ),
    BRIMHAVEN_MUSA_POINT(
            PortLocation.BRIMHAVEN,
            PortLocation.MUSA_POINT,
            // Sailing >= 25, used in 6 tasks
            new RelativeMove(0, 8),
            new RelativeMove(7, 7),
            new RelativeMove(33, 0),
            new RelativeMove(30, -30),
            new RelativeMove(76, 0),
            new RelativeMove(47, -47),
            new RelativeMove(13, 0),
            new RelativeMove(5, -5)
    ),

    BRIMHAVEN_PANDEMONIUM(
            PortLocation.BRIMHAVEN,
            PortLocation.PANDEMONIUM,
            // Sailing >= 25, used in 5 tasks
            new RelativeMove(0, 8),
            new RelativeMove(7, 7),
            new RelativeMove(33, 0),
            new RelativeMove(30, -30),
            new RelativeMove(76, 0),
            new RelativeMove(47, -47),
            new RelativeMove(13, 0),
            new RelativeMove(14, -14),
            new RelativeMove(0, -63),
            new RelativeMove(59, -59),
            new RelativeMove(37, 0),
            new RelativeMove(8, -8)
    ),

    BRIMHAVEN_PORT_KHAZARD(
            PortLocation.BRIMHAVEN,
            PortLocation.PORT_KHAZARD,
            // Sailing >= 30, used in 4 tasks
            new RelativeMove(0, 15),
            new RelativeMove(-7, 7),
            new RelativeMove(-26, 0),
            new RelativeMove(-33, -33)
    ),

    CATHERBY_ARDOUGNE(
            PortLocation.CATHERBY,
            PortLocation.ARDOUGNE,
            // Sailing >= 28, used in 6 tasks
            new RelativeMove(0, -27),
            new RelativeMove(-41, -41),
            new RelativeMove(0, -62),
            new RelativeMove(-19, -19)
    ),

    CATHERBY_MUSA_POINT(
            PortLocation.CATHERBY,
            PortLocation.MUSA_POINT,
            // Sailing >= 20, used in 4 tasks
            new RelativeMove(62, 0),
            new RelativeMove(20, -20),
            new RelativeMove(0, -42),
            new RelativeMove(4, -4),
            new RelativeMove(0, -105),
            new RelativeMove(60, -60),
            new RelativeMove(18, 0),
            new RelativeMove(5, -5)
    ),

    CATHERBY_PANDEMONIUM(
            PortLocation.CATHERBY,
            PortLocation.PANDEMONIUM,
            // Sailing >= 20, used in 4 tasks
            new RelativeMove(62, 0),
            new RelativeMove(20, -20),
            new RelativeMove(0, -42),
            new RelativeMove(4, -4),
            new RelativeMove(0, -105),
            new RelativeMove(60, -60),
            new RelativeMove(18, 0),
            new RelativeMove(13, -13),
            new RelativeMove(0, -67),
            new RelativeMove(75, -75),
            new RelativeMove(25, 0),
            new RelativeMove(5, -5)
    ),

    CATHERBY_PORT_KHAZARD(
            PortLocation.CATHERBY,
            PortLocation.PORT_KHAZARD,
            // Sailing >= 30, used in 5 tasks
            new RelativeMove(0, -28),
            new RelativeMove(-40, -40),
            new RelativeMove(0, -53),
            new RelativeMove(-68, -68)
    ),

    CATHERBY_PORT_SARIM(
            PortLocation.CATHERBY,
            PortLocation.PORT_SARIM,
            // Sailing >= 20, used in 6 tasks
            new RelativeMove(62, 0),
            new RelativeMove(20, -20),
            new RelativeMove(0, -42),
            new RelativeMove(4, -4),
            new RelativeMove(0, -105),
            new RelativeMove(60, -60),
            new RelativeMove(18, 0),
            new RelativeMove(10, -10),
            new RelativeMove(0, -51),
            new RelativeMove(13, -13),
            new RelativeMove(24, 0),
            new RelativeMove(49, 49)
    ),

    ARDOUGNE_PORT_KHAZARD(
            PortLocation.ARDOUGNE,
            PortLocation.PORT_KHAZARD,
            // Sailing >= 30, used in 5 tasks
            new RelativeMove(5, 0),
            new RelativeMove(7, -7),
            new RelativeMove(0, -60),
            new RelativeMove(6, -6)
    ),

    ARDOUGNE_RUINS_OF_UNKAH(
            PortLocation.ARDOUGNE,
            PortLocation.RUINS_OF_UNKAH,
            // Sailing >= 48, used in 2 tasks
            new RelativeMove(90, 0),
            new RelativeMove(34, -34),
            new RelativeMove(17, 0),
            new RelativeMove(12, -12),
            new RelativeMove(85, 0),
            new RelativeMove(55, -55),
            new RelativeMove(0, -42),
            new RelativeMove(121, -121),
            new RelativeMove(0, -20),
            new RelativeMove(43, -43),
            new RelativeMove(0, -108)
    ),

    ENTRANA_MUSA_POINT(
            PortLocation.ENTRANA,
            PortLocation.MUSA_POINT,
            // Sailing >= 36, used in 2 tasks
            new RelativeMove(0, -118),
            new RelativeMove(20, -20),
            new RelativeMove(20, 0),
            new RelativeMove(20, -20),
            new RelativeMove(17, 0),
            new RelativeMove(5, -5)
    ),

    MUSA_POINT_PANDEMONIUM(
            PortLocation.MUSA_POINT,
            PortLocation.PANDEMONIUM,
            // Sailing >= 10, used in 6 tasks
            new RelativeMove(5, 0),
            new RelativeMove(5, -5),
            new RelativeMove(0, -35),
            new RelativeMove(103, -103)
    ),

    MUSA_POINT_PORT_KHAZARD(
            PortLocation.MUSA_POINT,
            PortLocation.PORT_KHAZARD,
            // Sailing >= 30, used in 1 task
            new RelativeMove(0, 11),
            new RelativeMove(-57, 57),
            new RelativeMove(-93, 0),
            new RelativeMove(-36, 36),
            new RelativeMove(-62, 0),
            new RelativeMove(-29, -29)
    ),

    MUSA_POINT_PORT_SARIM(
            PortLocation.MUSA_POINT,
            PortLocation.PORT_SARIM,
            // Sailing >= 10, used in 6 tasks
            new RelativeMove(5, 0),
            new RelativeMove(5, -5),
            new RelativeMove(0, -35),
            new RelativeMove(14, -14),
            new RelativeMove(11, 0),
            new RelativeMove(56, 56)
    ),

    MUSA_POINT_RUINS_OF_UNKAH(
            PortLocation.MUSA_POINT,
            PortLocation.RUINS_OF_UNKAH,
            // Sailing >= 48, used in 0 tasks
            new RelativeMove(5, 0),
            new RelativeMove(5, -5),
            new RelativeMove(0, -35),
            new RelativeMove(103, -103),
            new RelativeMove(0, -20),
            new RelativeMove(45, -45),
            new RelativeMove(0, -117),
            new RelativeMove(20, 0)
    ),

    PANDEMONIUM_PORT_KHAZARD(
            PortLocation.PANDEMONIUM,
            PortLocation.PORT_KHAZARD,
            // Sailing >= 30, used in 2 tasks
            new RelativeMove(0, 20),
            new RelativeMove(-103, 103),
            new RelativeMove(0, 35),
            new RelativeMove(-5, 5),
            new RelativeMove(0, 10),
            new RelativeMove(-35, 35),
            new RelativeMove(-20, 0),
            new RelativeMove(-25, 25),
            new RelativeMove(-85, 0),
            new RelativeMove(-41, 41),
            new RelativeMove(-40, 0),
            new RelativeMove(-36, -36)
    ),

    PANDEMONIUM_RUINS_OF_UNKAH(
            PortLocation.PANDEMONIUM,
            PortLocation.RUINS_OF_UNKAH,
            // Sailing >= 48, used in 4 tasks
            new RelativeMove(0, -20),
            new RelativeMove(45, -45),
            new RelativeMove(0, -98),
            new RelativeMove(20, 0)
    ),

    PORT_KHAZARD_PORT_SARIM(
            PortLocation.PORT_KHAZARD,
            PortLocation.PORT_SARIM,
            // Sailing >= 30, used in 5 tasks
            new RelativeMove(0, 60),
            new RelativeMove(35, 35),
            new RelativeMove(45, 0),
            new RelativeMove(41, -41),
            new RelativeMove(85, 0),
            new RelativeMove(25, -25),
            new RelativeMove(20, 0),
            new RelativeMove(35, -35),
            new RelativeMove(0, -44),
            new RelativeMove(12, -12),
            new RelativeMove(22, 0),
            new RelativeMove(48, 48)
    ),

    PORT_KHAZARD_RUINS_OF_UNKAH(
            PortLocation.PORT_KHAZARD,
            PortLocation.RUINS_OF_UNKAH,
            // Sailing >= 48, used in 3 tasks
            new RelativeMove(0, 62),
            new RelativeMove(34, 34),
            new RelativeMove(56, 0),
            new RelativeMove(40, -40),
            new RelativeMove(86, 0),
            new RelativeMove(71, -71),
            new RelativeMove(0, -43),
            new RelativeMove(148, -148),
            new RelativeMove(0, -125),
            new RelativeMove(7, -7)
    ),

    RUINS_OF_UNKAH_SUMMER_SHORE(
            PortLocation.RUINS_OF_UNKAH,
            PortLocation.SUMMER_SHORE,
            // Sailing >= 48, used in 3 tasks
            new RelativeMove(-6, -6),
            new RelativeMove(0, -108),
            new RelativeMove(15, -15),
            new RelativeMove(0, -46),
            new RelativeMove(-85, -85),
            new RelativeMove(0, -166),
            new RelativeMove(31, -31)
    ),
    PORT_SARIM_PANDEMONIUM(
            PortLocation.PORT_SARIM,
            PortLocation.PANDEMONIUM,
            // Sailing >= 1, used in 6 tasks
            new RelativeMove(0, -43),
            new RelativeMove(-22, -22),
            new RelativeMove(0, -75),
            new RelativeMove(44, -44)
    ),
    PORT_SARIM_ARDOUGNE(
            PortLocation.PORT_SARIM,
            PortLocation.ARDOUGNE,
            // Sailing >= 28, used in 3 tasks
            new RelativeMove(0, -43),
            new RelativeMove(-50, -50),
            new RelativeMove(-22, 0),
            new RelativeMove(-8, 8),
            new RelativeMove(0, 40),
            new RelativeMove(-50, 50),
            new RelativeMove(-48, 0),
            new RelativeMove(-17, 17),
            new RelativeMove(-46, 0),
            new RelativeMove(-43, 43)
    ),
    CATHERBY_ENTRANA(
            PortLocation.CATHERBY,
            PortLocation.ENTRANA,
            // Sailing >= 36, used in 2 tasks
            new RelativeMove(60, 0),
            new RelativeMove(16, -16),
            new RelativeMove(0, -40),
            new RelativeMove(11, -11)
    ),
    CATHERBY_RUINS_OF_UNKAH(
            PortLocation.CATHERBY,
            PortLocation.RUINS_OF_UNKAH,
            // Sailing >= 48, used in 1 task
            new RelativeMove(62, 0),
            new RelativeMove(20, -20),
            new RelativeMove(0, -42),
            new RelativeMove(4, -4),
            new RelativeMove(0, -105),
            new RelativeMove(60, -60),
            new RelativeMove(18, 0),
            new RelativeMove(14, -14),
            new RelativeMove(0, -51),
            new RelativeMove(153, -153),
            new RelativeMove(0, -131),
            new RelativeMove(5, -5),
            new RelativeMove(10, 0)
    ),
    BRIMHAVEN_ARDOUGNE(
            PortLocation.BRIMHAVEN,
            PortLocation.ARDOUGNE,
            // Sailing >= 28, used in 4 tasks
            new RelativeMove(0, 16),
            new RelativeMove(-12, 12)
    ),
    PORT_KHAZARD_ENTRANA(
            PortLocation.PORT_KHAZARD,
            PortLocation.ENTRANA,
            // Sailing >= 36, used in 1 task
            new RelativeMove(0, 59),
            new RelativeMove(38, 38),
            new RelativeMove(66, 0),
            new RelativeMove(9, 9),
            new RelativeMove(0, 41),
            new RelativeMove(14, 14),
            new RelativeMove(63, 0),
            new RelativeMove(5, 5)
    ),
    PORT_KHAZARD_CORSAIR_COVE(
            PortLocation.PORT_KHAZARD,
            PortLocation.CORSAIR_COVE,
            // Sailing >= 40, used in 6 tasks
            new RelativeMove(0, -15),
            new RelativeMove(43, -43),
            new RelativeMove(0, -212),
            new RelativeMove(-48, -48)
    ),
    RUINS_OF_UNKAH_BRIMHAVEN(
            PortLocation.RUINS_OF_UNKAH,
            PortLocation.BRIMHAVEN,
            // Sailing >= 48, used in 1 task
            new RelativeMove(-14, 0),
            new RelativeMove(-6, 6),
            new RelativeMove(0, 132),
            new RelativeMove(-149, 149),
            new RelativeMove(0, 42),
            new RelativeMove(-47, 47),
            new RelativeMove(-39, 0),
            new RelativeMove(-18, 18),
            new RelativeMove(-61, 0),
            new RelativeMove(-29, 29),
            new RelativeMove(-20, 0),
            new RelativeMove(-6, -6)
    ),
    RUINS_OF_UNKAH_CORSAIR_COVE(
            PortLocation.RUINS_OF_UNKAH,
            PortLocation.CORSAIR_COVE,
            // Sailing >= 48, used in 0 tasks
            new RelativeMove(-72, 0),
            new RelativeMove(-15, -15),
            new RelativeMove(-78, 0),
            new RelativeMove(-28, 28),
            new RelativeMove(0, 11),
            new RelativeMove(-14, 14),
            new RelativeMove(-80, 0),
            new RelativeMove(-12, -12)
    ),
    PANDEMONIUM_ARDOUGNE(
            PortLocation.PANDEMONIUM,
            PortLocation.ARDOUGNE,
            // Sailing >= 28, used in 1 task
            new RelativeMove(0, 22),
            new RelativeMove(-105, 105),
            new RelativeMove(0, 42),
            new RelativeMove(-28, 28),
            new RelativeMove(-23, 0),
            new RelativeMove(-27, 27),
            new RelativeMove(-76, 0),
            new RelativeMove(-48, 48)
    ),
    MUSA_POINT_ARDOUGNE(
            PortLocation.MUSA_POINT,
            PortLocation.ARDOUGNE,
            // Sailing >= 28, used in 2 tasks
            new RelativeMove(0, 16),
            new RelativeMove(-28, 28),
            new RelativeMove(-23, 0),
            new RelativeMove(-27, 27),
            new RelativeMove(-76, 0),
            new RelativeMove(-42, 42)
    ),
    BRIMHAVEN_PORT_SARIM(
            PortLocation.BRIMHAVEN,
            PortLocation.PORT_SARIM,
            // Sailing >= 25, used in 3 tasks
            new RelativeMove(0, 7),
            new RelativeMove(8, 8),
            new RelativeMove(24, 0),
            new RelativeMove(26, -26),
            new RelativeMove(88, 0),
            new RelativeMove(65, -65),
            new RelativeMove(0, -40),
            new RelativeMove(18, -18),
            new RelativeMove(39, 0),
            new RelativeMove(34, 34)
    ),
    PORT_SARIM_RUINS_OF_UNKAH(
            PortLocation.PORT_SARIM,
            PortLocation.RUINS_OF_UNKAH,
            // Sailing >= 48, used in 4 tasks
            new RelativeMove(0, -50),
            new RelativeMove(-25, -25),
            new RelativeMove(0, -68),
            new RelativeMove(95, -95),
            new RelativeMove(0, -127),
            new RelativeMove(5, -5)
    ),
    PORT_PISCARILIUS_PORT_SARIM(
            PortLocation.PORT_PISCARILIUS,
            PortLocation.PORT_SARIM,
            // Sailing >= 15, used in 3 tasks
            // Slightly longer route to avoid aggressive NPCs
            new RelativeMove(108, -108),
            new RelativeMove(0, -423),
            new RelativeMove(-87, -87),
            new RelativeMove(0, -218),
            new RelativeMove(27, -27),
            new RelativeMove(77, 0),
            new RelativeMove(51, -51),
            new RelativeMove(70, 0),
            new RelativeMove(25, -25),
            new RelativeMove(0, -40),
            new RelativeMove(44, -44),
            new RelativeMove(253, 0),
            new RelativeMove(24, 24),
            new RelativeMove(138, 0),
            new RelativeMove(196, 196),
            new RelativeMove(217, 0),
            new RelativeMove(92, 92),
            new RelativeMove(0, 36),
            new RelativeMove(-52, 52),
            new RelativeMove(0, 60),
            new RelativeMove(28, 28)
    ),
    PORT_SARIM_VOID_KNIGHTS_OUTPOST(
            PortLocation.PORT_SARIM,
            PortLocation.VOID_KNIGHTS_OUTPOST,
            // Sailing >= 50, used in 1 task
            new RelativeMove(0, -38),
            new RelativeMove(-40, -40),
            new RelativeMove(0, -99),
            new RelativeMove(-10, -10),
            new RelativeMove(0, -103),
            new RelativeMove(-26, -26),
            new RelativeMove(-230, 0),
            new RelativeMove(-99, -99)
    ),
    CORSAIR_COVE_PANDEMONIUM(
            PortLocation.CORSAIR_COVE,
            PortLocation.PANDEMONIUM,
            // Sailing >= 40, used in 4 tasks
            new RelativeMove(276, 0),
            new RelativeMove(35, 35),
            new RelativeMove(78, 0),
            new RelativeMove(59, 59),
            new RelativeMove(22, 0),
            new RelativeMove(22, 22)
    ),
    CATHERBY_VOID_KNIGHTS_OUTPOST(
            PortLocation.CATHERBY,
            PortLocation.VOID_KNIGHTS_OUTPOST,
            // Sailing >= 50, used in 1 task
            new RelativeMove(0, -29),
            new RelativeMove(-44, -44),
            new RelativeMove(0, -53),
            new RelativeMove(-64, -64),
            new RelativeMove(0, -73),
            new RelativeMove(43, -43),
            new RelativeMove(0, -217),
            new RelativeMove(-80, -80)
    ),
    BRIMHAVEN_CORSAIR_COVE(
            PortLocation.BRIMHAVEN,
            PortLocation.CORSAIR_COVE,
            // Sailing >= 40, used in 5 tasks
            new RelativeMove(0, 17),
            new RelativeMove(-5, 5),
            new RelativeMove(-30, 0),
            new RelativeMove(-31, -31),
            new RelativeMove(0, -80),
            new RelativeMove(37, -37),
            new RelativeMove(0, -216),
            new RelativeMove(-45, -45)
    ),
    BRIMHAVEN_RED_ROCK(
            PortLocation.BRIMHAVEN,
            PortLocation.RED_ROCK,
            // Sailing >= 25, used in 2 tasks
            new RelativeMove(0, 15),
            new RelativeMove(-8, 8),
            new RelativeMove(-31, 0),
            new RelativeMove(-28, -28),
            new RelativeMove(0, -83),
            new RelativeMove(33, -33),
            new RelativeMove(0, -259),
            new RelativeMove(-70, -70),
            new RelativeMove(0, -55),
            new RelativeMove(177, -177),
            new RelativeMove(0, -16),
            new RelativeMove(-13, -13)
    ),
    BRIMHAVEN_SUMMER_SHORE(
            PortLocation.BRIMHAVEN,
            PortLocation.SUMMER_SHORE,
            // Sailing >= 45, used in 1 task
            new RelativeMove(0, 17),
            new RelativeMove(-6, 6),
            new RelativeMove(-29, 0),
            new RelativeMove(-30, -30),
            new RelativeMove(0, -80),
            new RelativeMove(37, -37),
            new RelativeMove(0, -202),
            new RelativeMove(106, -106),
            new RelativeMove(0, -49),
            new RelativeMove(76, -76),
            new RelativeMove(0, -73),
            new RelativeMove(164, -164),
            new RelativeMove(0, -32),
            new RelativeMove(38, -38)
    ),
    ARDOUGNE_PORT_TYRAS(
            PortLocation.ARDOUGNE,
            PortLocation.PORT_TYRAS,
            // Sailing >= 66, used in 5 tasks
            new RelativeMove(7, 0),
            new RelativeMove(10, -10),
            new RelativeMove(0, -101),
            new RelativeMove(41, -41),
            new RelativeMove(0, -234),
            new RelativeMove(-84, -84),
            new RelativeMove(0, -59),
            new RelativeMove(-47, -47),
            new RelativeMove(-240, 0),
            new RelativeMove(-24, -24),
            new RelativeMove(-144, 0),
            new RelativeMove(-105, 105),
            new RelativeMove(-69, 0),
            new RelativeMove(-33, 33),
            new RelativeMove(0, 107),
            new RelativeMove(138, 138),
            new RelativeMove(0, 65),
            new RelativeMove(8, 8)
    ),
    ARDOUGNE_PORT_PISCARILIUS(
            PortLocation.ARDOUGNE,
            PortLocation.PORT_PISCARILIUS,
            // Sailing >= 28, used in 2 tasks
            new RelativeMove(8, 0),
            new RelativeMove(10, -10),
            new RelativeMove(0, -106),
            new RelativeMove(39, -39),
            new RelativeMove(0, -234),
            new RelativeMove(-81, -81),
            new RelativeMove(0, -47),
            new RelativeMove(-138, -138),
            new RelativeMove(-267, 0),
            new RelativeMove(-155, 155),
            new RelativeMove(-70, 0),
            new RelativeMove(-35, 35),
            new RelativeMove(0, 82),
            new RelativeMove(-73, 73),
            new RelativeMove(0, 148),
            new RelativeMove(48, 48),
            new RelativeMove(0, 396),
            new RelativeMove(-111, 111)
    ),
    ARDOUGNE_RED_ROCK(
            PortLocation.ARDOUGNE,
            PortLocation.RED_ROCK,
            // Sailing >= 28, used in 2 tasks
            new RelativeMove(18, -18),
            new RelativeMove(0, -93),
            new RelativeMove(34, -34),
            new RelativeMove(0, -211),
            new RelativeMove(101, -101),
            new RelativeMove(0, -106),
            new RelativeMove(-41, -41),
            new RelativeMove(0, -75),
            new RelativeMove(43, -43),
            new RelativeMove(0, -7),
            new RelativeMove(-11, -11)
    ),
    CIVITAS_ILLA_FORTIS_PORT_KHAZARD(
            PortLocation.CIVITAS_ILLA_FORTIS,
            PortLocation.PORT_KHAZARD,
            // Sailing >= 38, used in 2 tasks
            new RelativeMove(-8, 8),
            new RelativeMove(0, 94),
            new RelativeMove(20, 20),
            new RelativeMove(132, 0),
            new RelativeMove(42, -42),
            new RelativeMove(0, -72),
            new RelativeMove(70, -70),
            new RelativeMove(0, -136),
            new RelativeMove(-45, -45),
            new RelativeMove(0, -102),
            new RelativeMove(38, -38),
            new RelativeMove(74, 0),
            new RelativeMove(20, -20),
            new RelativeMove(0, -76),
            new RelativeMove(43, -43),
            new RelativeMove(359, 0),
            new RelativeMove(215, 215),
            new RelativeMove(0, 273),
            new RelativeMove(-41, 41)
    ),
    CORSAIR_COVE_VOID_KNIGHTS_OUTPOST(
            PortLocation.CORSAIR_COVE,
            PortLocation.VOID_KNIGHTS_OUTPOST,
            // Sailing >= 50, used in 3 tasks
            new RelativeMove(32, 0),
            new RelativeMove(33, -33)
    ),
    CORSAIR_COVE_PORT_TYRAS(
            PortLocation.CORSAIR_COVE,
            PortLocation.PORT_TYRAS,
            // Sailing >= 66, used in 3 tasks
            new RelativeMove(24, 0),
            new RelativeMove(34, -34),
            new RelativeMove(0, -77),
            new RelativeMove(-49, -49),
            new RelativeMove(-419, 0),
            new RelativeMove(-68, 68),
            new RelativeMove(-75, 0),
            new RelativeMove(-54, 54),
            new RelativeMove(0, 82),
            new RelativeMove(142, 142),
            new RelativeMove(0, 75),
            new RelativeMove(10, 10)
    ),
    CORSAIR_COVE_PORT_PISCARILIUS(
            PortLocation.CORSAIR_COVE,
            PortLocation.PORT_PISCARILIUS,
            // Sailing >= 40, used in 2 tasks
            new RelativeMove(24, 0),
            new RelativeMove(34, -34),
            new RelativeMove(0, -77),
            new RelativeMove(-49, -49),
            new RelativeMove(-419, 0),
            new RelativeMove(-68, 68),
            new RelativeMove(-75, 0),
            new RelativeMove(-54, 54),
            new RelativeMove(0, 82),
            new RelativeMove(142, 142),
            new RelativeMove(0, 75),
            new RelativeMove(10, 10)
    ),
    CAIRN_ISLE_CORSAIR_COVE(
            PortLocation.CAIRN_ISLE,
            PortLocation.CORSAIR_COVE,
            // Sailing >= 42, used in 2 tasks
            new RelativeMove(0, -42),
            new RelativeMove(-66, -66)
    ),
    RED_ROCK_RUINS_OF_UNKAH(
            PortLocation.RED_ROCK,
            PortLocation.RUINS_OF_UNKAH,
            // Sailing >= 48, used in 3 tasks
            new RelativeMove(19, 19),
            new RelativeMove(70, 0),
            new RelativeMove(10, -10),
            new RelativeMove(118, 0),
            new RelativeMove(118, 118),
            new RelativeMove(0, 57),
            new RelativeMove(-12, 12),
            new RelativeMove(0, 102),
            new RelativeMove(6, 6)
    ),
    RUINS_OF_UNKAH_VOID_KNIGHTS_OUTPOST(
            PortLocation.RUINS_OF_UNKAH,
            PortLocation.VOID_KNIGHTS_OUTPOST,
            // Sailing >= 50, used in 2 tasks
            new RelativeMove(-8, -8),
            new RelativeMove(0, -103),
            new RelativeMove(17, -17),
            new RelativeMove(0, -52),
            new RelativeMove(-48, -48),
            new RelativeMove(-290, 0),
            new RelativeMove(-87, 87)
    ),
    RED_ROCK_VOID_KNIGHTS_OUTPOST(
            PortLocation.RED_ROCK,
            PortLocation.VOID_KNIGHTS_OUTPOST,
            // Sailing >= 50, used in 2 tasks
            new RelativeMove(14, 14),
            new RelativeMove(0, 69),
            new RelativeMove(-90, 90)
    ),
    SUMMER_SHORE_VOID_KNIGHTS_OUTPOST(
            PortLocation.SUMMER_SHORE,
            PortLocation.VOID_KNIGHTS_OUTPOST,
            // Sailing >= 50, used in 2 tasks
            new RelativeMove(-160, 0),
            new RelativeMove(-316, 316)
    ),
    DEEPFIN_POINT_VOID_KNIGHTS_OUTPOST(
            PortLocation.DEEPFIN_POINT,
            PortLocation.VOID_KNIGHTS_OUTPOST,
            // Sailing >= 67, used in 2 tasks
            new RelativeMove(68, -68),
            new RelativeMove(110, 0),
            new RelativeMove(64, -64),
            new RelativeMove(356, 0),
            new RelativeMove(63, 63)
    ),
    LANDS_END_PRIFDDINAS(
            PortLocation.LANDS_END,
            PortLocation.PRIFDDINAS,
            // Sailing >= 70, used in 1 task
            new RelativeMove(12, -12),
            new RelativeMove(373, 0),
            new RelativeMove(36, -36),
            new RelativeMove(156, 0),
            new RelativeMove(38, -38)
    ),
    LANDS_END_PISCATORIS(
            PortLocation.LANDS_END,
            PortLocation.PISCATORIS,
            // Sailing >= 75, used in 2 tasks
            new RelativeMove(6, -6),
            new RelativeMove(392, 0),
            new RelativeMove(95, 95),
            new RelativeMove(38, 0),
            new RelativeMove(20, 20),
            new RelativeMove(0, 42),
            new RelativeMove(48, 48),
            new RelativeMove(78, 0),
            new RelativeMove(68, 68),
            new RelativeMove(36, 0),
            new RelativeMove(8, 8)
    ),
    CORSAIR_COVE_LANDS_END(
            PortLocation.CORSAIR_COVE,
            PortLocation.LANDS_END,
            // Sailing >= 40, used in 1 task
            new RelativeMove(55, -55),
            new RelativeMove(0, -48),
            new RelativeMove(-129, -129),
            new RelativeMove(-275, 0),
            new RelativeMove(-144, 144),
            new RelativeMove(-93, 0),
            new RelativeMove(-22, 22),
            new RelativeMove(0, 99),
            new RelativeMove(-71, 71),
            new RelativeMove(0, 180),
            new RelativeMove(-245, 245),
            new RelativeMove(-139, 0),
            new RelativeMove(-12, 12)
    ),
    LANDS_END_PORT_PISCARILIUS(
            PortLocation.LANDS_END,
            PortLocation.PORT_PISCARILIUS,
            // Sailing >= 15, used in 3 tasks
            new RelativeMove(9, -9),
            new RelativeMove(294, 0),
            new RelativeMove(74, 74),
            new RelativeMove(0, 139),
            new RelativeMove(-43, 43)
    ),
    PORT_PISCARILIUS_PORT_ROBERTS(
            PortLocation.PORT_PISCARILIUS,
            PortLocation.PORT_ROBERTS,
            // Sailing >= 50, used in 3 tasks
            new RelativeMove(0, -18),
            new RelativeMove(69, -69),
            new RelativeMove(0, -214),
            new RelativeMove(-56, -56)
    ),
    PISCATORIS_PORT_PISCARILIUS(
            PortLocation.PISCATORIS,
            PortLocation.PORT_PISCARILIUS,
            // Sailing >= 75, used in 3 tasks
            new RelativeMove(-22, -22),
            new RelativeMove(-39, 0),
            new RelativeMove(-63, -63),
            new RelativeMove(-148, 0),
            new RelativeMove(-19, -19),
            new RelativeMove(-68, 0)
    ),
    HOSIDIUS_PORT_PISCARILIUS(
            PortLocation.HOSIDIUS,
            PortLocation.PORT_PISCARILIUS,
            // Sailing >= 15, used in 2 tasks
            new RelativeMove(0, -25),
            new RelativeMove(21, -21),
            new RelativeMove(131, 0),
            new RelativeMove(22, 22),
            new RelativeMove(0, 174),
            new RelativeMove(-55, 55)
    ),
    LUNAR_ISLE_PORT_PISCARILIUS(
            PortLocation.LUNAR_ISLE,
            PortLocation.PORT_PISCARILIUS,
            // Sailing >= 76, used in 5 tasks
            new RelativeMove(12, 0),
            new RelativeMove(3, -3),
            new RelativeMove(0, -21),
            new RelativeMove(-13, -13),
            new RelativeMove(-6, 0),
            new RelativeMove(-6, -6),
            new RelativeMove(-213, 0),
            new RelativeMove(-69, -69),
            new RelativeMove(0, -75),
            new RelativeMove(-13, -13)
    ),
    PORT_PISCARILIUS_PORT_TYRAS(
            PortLocation.PORT_PISCARILIUS,
            PortLocation.PORT_TYRAS,
            // Sailing >= 66, used in 3 tasks
            new RelativeMove(47, -47),
            new RelativeMove(0, -182),
            new RelativeMove(61, -61),
            new RelativeMove(0, -211),
            new RelativeMove(35, -35),
            new RelativeMove(90, 0),
            new RelativeMove(30, -30)
    ),
    PORT_PISCARILIUS_RELLEKKA(
            PortLocation.PORT_PISCARILIUS,
            PortLocation.RELLEKKA,
            // Sailing >= 62, used in 3 tasks
            new RelativeMove(113, 0),
            new RelativeMove(42, 42),
            new RelativeMove(195, 0),
            new RelativeMove(33, 33),
            new RelativeMove(236, 0),
            new RelativeMove(47, -47)
    ),
    PORT_PISCARILIUS_PRIFDDINAS(
            PortLocation.PORT_PISCARILIUS,
            PortLocation.PRIFDDINAS,
            // Sailing >= 70, used in 2 tasks
            new RelativeMove(62, -62),
            new RelativeMove(0, -236),
            new RelativeMove(29, -29),
            new RelativeMove(155, 0),
            new RelativeMove(35, -35)
    ),
    ALDARIN_CIVITAS_ILLA_FORTIS(
            PortLocation.ALDARIN,
            PortLocation.CIVITAS_ILLA_FORTIS,
            // Sailing >= 46, used in 3 tasks
            new RelativeMove(18, 0),
            new RelativeMove(97, -97),
            new RelativeMove(170, 0),
            new RelativeMove(74, 74),
            new RelativeMove(0, 67),
            new RelativeMove(76, 76),
            new RelativeMove(0, 48),
            new RelativeMove(-62, 62),
            new RelativeMove(-57, 0),
            new RelativeMove(-9, -9),
            new RelativeMove(0, -48),
            new RelativeMove(6, -6)
    ),
    CIVITAS_ILLA_FORTIS_PORT_PISCARILIUS(
            PortLocation.CIVITAS_ILLA_FORTIS,
            PortLocation.PORT_PISCARILIUS,
            // Sailing >= 38, used in 3 tasks
            new RelativeMove(-7, 7),
            new RelativeMove(0, 84),
            new RelativeMove(135, 135),
            new RelativeMove(0, 239),
            new RelativeMove(-52, 52)
    ),
    CIVITAS_ILLA_FORTIS_PORT_ROBERTS(
            PortLocation.CIVITAS_ILLA_FORTIS,
            PortLocation.PORT_ROBERTS,
            // Sailing >= 50, used in 3 tasks
            new RelativeMove(-8, 8),
            new RelativeMove(0, 103),
            new RelativeMove(53, 53)
    ),
    CIVITAS_ILLA_FORTIS_DEEPFIN_POINT(
            PortLocation.CIVITAS_ILLA_FORTIS,
            PortLocation.DEEPFIN_POINT,
            // Sailing >= 67, used in 2 tasks
            new RelativeMove(-8, 8),
            new RelativeMove(0, 41),
            new RelativeMove(9, 9),
            new RelativeMove(63, 0),
            new RelativeMove(83, -83),
            new RelativeMove(0, -179),
            new RelativeMove(-20, -20),
            new RelativeMove(0, -143),
            new RelativeMove(25, -25)
    ),
    CIVITAS_ILLA_FORTIS_PRIFDDINAS(
            PortLocation.CIVITAS_ILLA_FORTIS,
            PortLocation.PRIFDDINAS,
            // Sailing >= 70, used in 4 tasks
            new RelativeMove(-9, 9),
            new RelativeMove(0, 83),
            new RelativeMove(125, 125),
            new RelativeMove(198, 0),
            new RelativeMove(42, -42)
    ),
    CIVITAS_ILLA_FORTIS_SUNSET_COAST(
            PortLocation.CIVITAS_ILLA_FORTIS,
            PortLocation.SUNSET_COAST,
            // Sailing >= 44, used in 2 tasks
            new RelativeMove(-8, 8),
            new RelativeMove(0, 41),
            new RelativeMove(10, 10),
            new RelativeMove(62, 0),
            new RelativeMove(59, -59),
            new RelativeMove(0, -62),
            new RelativeMove(-78, -78),
            new RelativeMove(0, -53),
            new RelativeMove(-72, -72),
            new RelativeMove(-171, 0),
            new RelativeMove(-65, 65)
    ),
    PORT_ROBERTS_PORT_TYRAS(
            PortLocation.PORT_ROBERTS,
            PortLocation.PORT_TYRAS,
            // Sailing >= 66, used in 2 tasks
            new RelativeMove(0, -27),
            new RelativeMove(11, -11),
            new RelativeMove(53, 0),
            new RelativeMove(34, -34),
            new RelativeMove(0, -63),
            new RelativeMove(33, -33),
            new RelativeMove(84, 0),
            new RelativeMove(24, -24)
    ),
    DEEPFIN_POINT_PORT_TYRAS(
            PortLocation.DEEPFIN_POINT,
            PortLocation.PORT_TYRAS,
            // Sailing >= 67, used in 3 tasks
            new RelativeMove(-27, 27),
            new RelativeMove(0, 48),
            new RelativeMove(145, 145),
            new RelativeMove(25, 0),
            new RelativeMove(67, 67),
            new RelativeMove(0, 54),
            new RelativeMove(8, 8)
    ),
    PORT_TYRAS_PRIFDDINAS(
            PortLocation.PORT_TYRAS,
            PortLocation.PRIFDDINAS,
            // Sailing >= 70, used in 4 tasks
            new RelativeMove(-8, 8),
            new RelativeMove(0, 12),
            new RelativeMove(18, 18),
            new RelativeMove(0, 158)
    ),
    LANDS_END_PORT_TYRAS(
            PortLocation.LANDS_END,
            PortLocation.PORT_TYRAS,
            // Sailing >= 66, used in 2 tasks
            new RelativeMove(0, -14),
            new RelativeMove(25, -25),
            new RelativeMove(135, 0),
            new RelativeMove(98, -98),
            new RelativeMove(145, 0),
            new RelativeMove(44, -44),
            new RelativeMove(0, -56),
            new RelativeMove(30, -30),
            new RelativeMove(84, 0),
            new RelativeMove(23, -23)
    ),
    ALDARIN_PORT_TYRAS(
            PortLocation.ALDARIN,
            PortLocation.PORT_TYRAS,
            // Sailing >= 66, used in 1 task
            new RelativeMove(11, 0),
            new RelativeMove(98, -98),
            new RelativeMove(161, 0),
            new RelativeMove(56, 56),
            new RelativeMove(231, 0),
            new RelativeMove(109, 109),
            new RelativeMove(0, 62),
            new RelativeMove(9, 9)
    ),
    PORT_ROBERTS_PRIFDDINAS(
            PortLocation.PORT_ROBERTS,
            PortLocation.PRIFDDINAS,
            // Sailing >= 70, used in 3 tasks
            new RelativeMove(0, 16),
            new RelativeMove(38, 38),
            new RelativeMove(161, 0),
            new RelativeMove(42, -42)
    ),
    ARDOUGNE_PRIFDDINAS(
            PortLocation.ARDOUGNE,
            PortLocation.PRIFDDINAS,
            // Sailing >= 70, used in 3 tasks
            new RelativeMove(8, 0),
            new RelativeMove(10, -10),
            new RelativeMove(0, -106),
            new RelativeMove(31, -31),
            new RelativeMove(0, -236),
            new RelativeMove(-69, -69),
            new RelativeMove(0, -75),
            new RelativeMove(-49, -49),
            new RelativeMove(-239, 0),
            new RelativeMove(-12, 12),
            new RelativeMove(-98, 0),
            new RelativeMove(-14, -14),
            new RelativeMove(-58, 0),
            new RelativeMove(-85, 85),
            new RelativeMove(-88, 0),
            new RelativeMove(-26, 26),
            new RelativeMove(0, 101),
            new RelativeMove(142, 142),
            new RelativeMove(0, 86),
            new RelativeMove(25, 25),
            new RelativeMove(0, 148),
            new RelativeMove(10, 10)
    ),
    DEEPFIN_POINT_PRIFDDINAS(
            PortLocation.DEEPFIN_POINT,
            PortLocation.PRIFDDINAS,
            // Sailing >= 70, used in 2 tasks
            new RelativeMove(-37, 37),
            new RelativeMove(0, 27),
            new RelativeMove(168, 168),
            new RelativeMove(0, 189),
            new RelativeMove(99, 99),
            new RelativeMove(0, 28),
            new RelativeMove(5, 5)
    ),
    ALDARIN_PRIFDDINAS(
            PortLocation.ALDARIN,
            PortLocation.PRIFDDINAS,
            // Sailing >= 70, used in 2 tasks
            new RelativeMove(17, 0),
            new RelativeMove(97, -97),
            new RelativeMove(173, 0),
            new RelativeMove(38, 38),
            new RelativeMove(221, 0),
            new RelativeMove(127, 127),
            new RelativeMove(0, 86),
            new RelativeMove(26, 26),
            new RelativeMove(0, 139),
            new RelativeMove(5, 5)
    ),
    LUNAR_ISLE_PRIFDDINAS(
            PortLocation.LUNAR_ISLE,
            PortLocation.PRIFDDINAS,
            // Sailing >= 76, used in 4 tasks
            new RelativeMove(8, 0),
            new RelativeMove(8, -8),
            new RelativeMove(0, -74),
            new RelativeMove(34, -34),
            new RelativeMove(0, -128),
            new RelativeMove(-24, -24),
            new RelativeMove(-78, 0),
            new RelativeMove(-43, -43),
            new RelativeMove(0, -67),
            new RelativeMove(11, -11),
            new RelativeMove(0, -119),
            new RelativeMove(54, -54)
    ),
    ETCETERIA_RELLEKKA(
            PortLocation.ETCETERIA,
            PortLocation.RELLEKKA,
            // Sailing >= 65, used in 6 tasks
            new RelativeMove(0, -10),
            new RelativeMove(18, -18)
    ),
    NEITIZNOT_RELLEKKA(
            PortLocation.NEITIZNOT,
            PortLocation.RELLEKKA,
            // Sailing >= 68, used in 3 tasks
            new RelativeMove(0, -15),
            new RelativeMove(13, -13),
            new RelativeMove(152, 0),
            new RelativeMove(45, -45)
    ),
    RELLEKKA_SUNSET_COAST(
            PortLocation.RELLEKKA,
            PortLocation.SUNSET_COAST,
            // Sailing >= 62, used in 3 tasks
            new RelativeMove(-116, 0),
            new RelativeMove(-45, 45),
            new RelativeMove(-219, 0),
            new RelativeMove(-34, -34),
            new RelativeMove(0, -83),
            new RelativeMove(-24, -24),
            new RelativeMove(-186, 0),
            new RelativeMove(-46, -46),
            new RelativeMove(0, -398),
            new RelativeMove(-166, -166),
            new RelativeMove(0, -56),
            new RelativeMove(-67, -67),
            new RelativeMove(-160, 0),
            new RelativeMove(-61, 61)
    ),
    PORT_ROBERTS_RELLEKKA(
            PortLocation.PORT_ROBERTS,
            PortLocation.RELLEKKA,
            // Sailing >= 62, used in 2 tasks
            new RelativeMove(0, 19),
            new RelativeMove(193, 193),
            new RelativeMove(0, 24),
            new RelativeMove(64, 64),
            new RelativeMove(74, 0),
            new RelativeMove(45, 45),
            new RelativeMove(0, 87),
            new RelativeMove(15, 15),
            new RelativeMove(220, 0),
            new RelativeMove(45, -45)
    ),
    PISCATORIS_RELLEKKA(
            PortLocation.PISCATORIS,
            PortLocation.RELLEKKA,
            // Sailing >= 75, used in 2 tasks
            new RelativeMove(0, 15),
            new RelativeMove(17, 17),
            new RelativeMove(182, 0),
            new RelativeMove(12, -12)
    ),
    JATIZSO_RELLEKKA(
            PortLocation.JATIZSO,
            PortLocation.RELLEKKA,
            // Sailing >= 68, used in 2 tasks
            new RelativeMove(34, 0),
            new RelativeMove(67, -67)
    ),
    PORT_TYRAS_RELLEKKA(
            PortLocation.PORT_TYRAS,
            PortLocation.RELLEKKA,
            // Sailing >= 66, used in 1 task
            new RelativeMove(0, 55),
            new RelativeMove(-118, 118),
            new RelativeMove(0, 87),
            new RelativeMove(42, 42),
            new RelativeMove(0, 142),
            new RelativeMove(48, 48),
            new RelativeMove(72, 0),
            new RelativeMove(44, 44),
            new RelativeMove(0, 89),
            new RelativeMove(14, 14),
            new RelativeMove(224, 0),
            new RelativeMove(45, -45)
    ),
    ETCETERIA_JATIZSO(
            PortLocation.ETCETERIA,
            PortLocation.JATIZSO,
            // Sailing >= 68, used in 3 tasks
            new RelativeMove(0, -12),
            new RelativeMove(-29, -29),
            new RelativeMove(-86, 0),
            new RelativeMove(-19, -19)
    ),
    ETCETERIA_PORT_ROBERTS(
            PortLocation.ETCETERIA,
            PortLocation.PORT_ROBERTS,
            // Sailing >= 65, used in 3 tasks
            new RelativeMove(0, -11),
            new RelativeMove(-32, -32),
            new RelativeMove(-94, 0),
            new RelativeMove(-33, -33),
            new RelativeMove(-200, 0),
            new RelativeMove(-47, -47),
            new RelativeMove(-120, 0),
            new RelativeMove(-13, 13),
            new RelativeMove(-69, 0),
            new RelativeMove(-108, -108),
            new RelativeMove(0, -252),
            new RelativeMove(-38, -38)
    ),
    ETCETERIA_PORT_PISCARILIUS(
            PortLocation.ETCETERIA,
            PortLocation.PORT_PISCARILIUS,
            // Sailing >= 65, used in 2 tasks
            new RelativeMove(0, -18),
            new RelativeMove(-26, -26),
            new RelativeMove(-101, 0),
            new RelativeMove(-34, -34),
            new RelativeMove(-198, 0),
            new RelativeMove(-45, -45),
            new RelativeMove(-133, 0),
            new RelativeMove(-19, 19),
            new RelativeMove(-45, 0),
            new RelativeMove(-51, -51)
    ),
    ETCETERIA_NEITIZNOT(
            PortLocation.ETCETERIA,
            PortLocation.NEITIZNOT,
            // Sailing >= 68, used in 2 tasks
            new RelativeMove(0, -22),
            new RelativeMove(-22, -22),
            new RelativeMove(-122, 0),
            new RelativeMove(-27, -27),
            new RelativeMove(-133, 0),
            new RelativeMove(-6, 6)
    ),
    ETCETERIA_SUNSET_COAST(
            PortLocation.ETCETERIA,
            PortLocation.SUNSET_COAST,
            // Sailing >= 65, used in 2 tasks
            new RelativeMove(-63, -63),
            new RelativeMove(-100, 0),
            new RelativeMove(-20, -20),
            new RelativeMove(-184, 0),
            new RelativeMove(-31, -31),
            new RelativeMove(0, -87),
            new RelativeMove(-31, -31),
            new RelativeMove(-191, 0),
            new RelativeMove(-34, -34),
            new RelativeMove(0, -402),
            new RelativeMove(-118, -118),
            new RelativeMove(-31, 0),
            new RelativeMove(-15, -15),
            new RelativeMove(0, -84),
            new RelativeMove(-71, -71),
            new RelativeMove(-126, 0),
            new RelativeMove(-22, 22),
            new RelativeMove(0, 25),
            new RelativeMove(-9, 9),
            new RelativeMove(-33, 0),
            new RelativeMove(-27, 27)
    ),
    ETCETERIA_PISCATORIS(
            PortLocation.ETCETERIA,
            PortLocation.PISCATORIS,
            // Sailing >= 75, used in 2 tasks
            new RelativeMove(0, -20),
            new RelativeMove(-26, -26),
            new RelativeMove(-100, 0),
            new RelativeMove(-51, -51),
            new RelativeMove(-103, 0),
            new RelativeMove(-32, -32)
    ),
    ETCETERIA_HOSIDIUS(
            PortLocation.ETCETERIA,
            PortLocation.HOSIDIUS,
            // Sailing >= 65, used in 1 task
            new RelativeMove(0, -16),
            new RelativeMove(-27, -27),
            new RelativeMove(-99, 0),
            new RelativeMove(-30, -30),
            new RelativeMove(-198, 0),
            new RelativeMove(-43, -43),
            new RelativeMove(0, -88),
            new RelativeMove(-30, -30),
            new RelativeMove(-195, 0),
            new RelativeMove(-81, -81),
            new RelativeMove(-15, -15),
            new RelativeMove(0, -44),
            new RelativeMove(-32, -32),
            new RelativeMove(-62, 0),
            new RelativeMove(-21, 21),
            new RelativeMove(-21, 0),
            new RelativeMove(-4, -4)
    ),
    LUNAR_ISLE_PISCATORIS(
            PortLocation.LUNAR_ISLE,
            PortLocation.PISCATORIS,
            // Sailing >= 76, used in 3 tasks
            new RelativeMove(3, 0),
            new RelativeMove(30, -30),
            new RelativeMove(29, 0),
            new RelativeMove(81, -81)
    ),
    DEEPFIN_POINT_LUNAR_ISLE(
            PortLocation.DEEPFIN_POINT,
            PortLocation.LUNAR_ISLE,
            // Sailing >= 76, used in 2 tasks
            new RelativeMove(-31, 31),
            new RelativeMove(0, 134),
            new RelativeMove(0, 45),
            new RelativeMove(44, 44),
            new RelativeMove(0, 132),
            new RelativeMove(21, 21),
            new RelativeMove(0, 405),
            new RelativeMove(42, 42),
            new RelativeMove(180, 0),
            new RelativeMove(29, 29),
            new RelativeMove(0, 128),
            new RelativeMove(-41, 41),
            new RelativeMove(0, 48),
            new RelativeMove(5, 5),
            new RelativeMove(0, 11),
            new RelativeMove(-13, 13)
    ),
    LUNAR_ISLE_PORT_ROBERTS(
            PortLocation.LUNAR_ISLE,
            PortLocation.PORT_ROBERTS,
            // Sailing >= 76, used in 2 tasks
            new RelativeMove(3, 0),
            new RelativeMove(14, -14),
            new RelativeMove(0, -8),
            new RelativeMove(-15, -15),
            new RelativeMove(-22, 0),
            new RelativeMove(-6, -6),
            new RelativeMove(-68, 0),
            new RelativeMove(-38, -38),
            new RelativeMove(0, -52),
            new RelativeMove(-117, -117),
            new RelativeMove(0, -242),
            new RelativeMove(-50, -50)
    ),
    CIVITAS_ILLA_FORTIS_LUNAR_ISLE(
            PortLocation.CIVITAS_ILLA_FORTIS,
            PortLocation.LUNAR_ISLE,
            // Sailing >= 76, used in 2 tasks
            new RelativeMove(-9, 9),
            new RelativeMove(0, 73),
            new RelativeMove(285, 285),
            new RelativeMove(0, 32),
            new RelativeMove(67, 67),
            new RelativeMove(79, 0),
            new RelativeMove(14, 14),
            new RelativeMove(0, 144),
            new RelativeMove(-33, 33),
            new RelativeMove(0, 67),
            new RelativeMove(-13, 13)
    ),
    LUNAR_ISLE_RED_ROCK(
            PortLocation.LUNAR_ISLE,
            PortLocation.RED_ROCK,
            // Sailing >= 76, used in 2 tasks
            new RelativeMove(3, 0),
            new RelativeMove(12, -12),
            new RelativeMove(0, -72),
            new RelativeMove(36, -36),
            new RelativeMove(0, -132),
            new RelativeMove(-27, -27),
            new RelativeMove(-76, 0),
            new RelativeMove(-41, -41),
            new RelativeMove(0, -164),
            new RelativeMove(-102, -102),
            new RelativeMove(0, -163),
            new RelativeMove(40, -40),
            new RelativeMove(0, -167),
            new RelativeMove(-25, -25),
            new RelativeMove(0, -96),
            new RelativeMove(55, -55),
            new RelativeMove(68, 0),
            new RelativeMove(167, -167),
            new RelativeMove(270, 0),
            new RelativeMove(92, -92),
            new RelativeMove(181, 0),
            new RelativeMove(4, 4)
    ),
    ALDARIN_LUNAR_ISLE(
            PortLocation.ALDARIN,
            PortLocation.LUNAR_ISLE,
            // Sailing >= 76, used in 1 task
            new RelativeMove(29, 0),
            new RelativeMove(41, -41),
            new RelativeMove(42, 0),
            new RelativeMove(10, -10),
            new RelativeMove(0, -25),
            new RelativeMove(21, -21),
            new RelativeMove(142, 0),
            new RelativeMove(92, 92),
            new RelativeMove(0, 72),
            new RelativeMove(126, 126),
            new RelativeMove(0, 235),
            new RelativeMove(-48, 48),
            new RelativeMove(0, 183),
            new RelativeMove(156, 156),
            new RelativeMove(0, 8),
            new RelativeMove(41, 41),
            new RelativeMove(43, 0),
            new RelativeMove(10, 10),
            new RelativeMove(7, 0),
            new RelativeMove(6, 6),
            new RelativeMove(0, 11),
            new RelativeMove(-13, 13)
    ),
    PORT_SARIM_RELLEKKA(
            PortLocation.PORT_SARIM,
            PortLocation.RELLEKKA,
            // Sailing >= 62, used in 1 task
            new RelativeMove(0, -45),
            new RelativeMove(-47, -47),
            new RelativeMove(-29, 0),
            new RelativeMove(-12, 12),
            new RelativeMove(0, 40),
            new RelativeMove(-62, 62),
            new RelativeMove(-89, 0),
            new RelativeMove(-36, 36),
            new RelativeMove(-61, 0),
            new RelativeMove(-32, -32),
            new RelativeMove(0, -72),
            new RelativeMove(38, -38),
            new RelativeMove(0, -276),
            new RelativeMove(-148, -148),
            new RelativeMove(-410, 0),
            new RelativeMove(-73, 73),
            new RelativeMove(-66, 0),
            new RelativeMove(-44, 44),
            new RelativeMove(0, 78),
            new RelativeMove(-51, 51),
            new RelativeMove(0, 200),
            new RelativeMove(26, 26),
            new RelativeMove(0, 292),
            new RelativeMove(156, 156),
            new RelativeMove(73, 0),
            new RelativeMove(40, 40),
            new RelativeMove(0, 90),
            new RelativeMove(19, 19),
            new RelativeMove(218, 0),
            new RelativeMove(46, -46)
    ),
    PANDEMONIUM_CAIRN_ISLE(
            PortLocation.PANDEMONIUM,
            PortLocation.CAIRN_ISLE,
            // Sailing >= 42, used in 1 task
            new RelativeMove(0, -21),
            new RelativeMove(-58, -58),
            new RelativeMove(-20, 0),
            new RelativeMove(-30, -30),
            new RelativeMove(-200, 0),
            new RelativeMove(-25, 25)
    ),
    MUSA_POINT_CORSAIR_COVE(
            PortLocation.MUSA_POINT,
            PortLocation.CORSAIR_COVE,
            // Sailing >= 40, used in 1 task
            new RelativeMove(0, 8),
            new RelativeMove(-61, 61),
            new RelativeMove(-85, 0),
            new RelativeMove(-41, 41),
            new RelativeMove(-59, 0),
            new RelativeMove(-30, -30),
            new RelativeMove(0, -82),
            new RelativeMove(36, -36),
            new RelativeMove(0, -209),
            new RelativeMove(-55, -55)
    ),
    MUSA_POINT_SUMMER_SHORE(
            PortLocation.MUSA_POINT,
            PortLocation.SUMMER_SHORE,
            // Sailing >= 45, used in 1 task
            new RelativeMove(0, -27),
            new RelativeMove(42, -42),
            new RelativeMove(0, -48),
            new RelativeMove(-3, -3),
            new RelativeMove(0, -135),
            new RelativeMove(-22, -22),
            new RelativeMove(0, -46),
            new RelativeMove(40, -40),
            new RelativeMove(0, -69),
            new RelativeMove(49, -49),
            new RelativeMove(0, -252),
            new RelativeMove(15, -15),
            new RelativeMove(10, 0),
            new RelativeMove(5, -5),
            new RelativeMove(3, 0),
            new RelativeMove(5, -5),
            new RelativeMove(6, 0),
            new RelativeMove(21, -21)
    ),
    MUSA_POINT_PORT_TYRAS(
            PortLocation.MUSA_POINT,
            PortLocation.PORT_TYRAS,
            // Sailing >= 66, used in 1 task
            new RelativeMove(0, -27),
            new RelativeMove(42, -42),
            new RelativeMove(0, -48),
            new RelativeMove(-3, -3),
            new RelativeMove(0, -128),
            new RelativeMove(-21, -21),
            new RelativeMove(-260, 0),
            new RelativeMove(-75, -75),
            new RelativeMove(0, -77),
            new RelativeMove(-40, -40),
            new RelativeMove(-450, 0),
            new RelativeMove(-75, 75),
            new RelativeMove(-53, 0),
            new RelativeMove(-40, 40),
            new RelativeMove(0, 94),
            new RelativeMove(151, 151)
    ),
    CATHERBY_PORT_PISCARILIUS(
            PortLocation.CATHERBY,
            PortLocation.PORT_PISCARILIUS,
            // Sailing >= 20, used in 1 task
            new RelativeMove(0, -16),
            new RelativeMove(-41, -41),
            new RelativeMove(0, -64),
            new RelativeMove(-67, -67),
            new RelativeMove(0, -82),
            new RelativeMove(31, -31),
            new RelativeMove(0, -280),
            new RelativeMove(-140, -140),
            new RelativeMove(-421, 0),
            new RelativeMove(-74, 74),
            new RelativeMove(-66, 0),
            new RelativeMove(-38, 38),
            new RelativeMove(0, 85),
            new RelativeMove(-57, 57),
            new RelativeMove(0, 173),
            new RelativeMove(34, 34),
            new RelativeMove(0, 387),
            new RelativeMove(-112, 112)
    ),
    BRIMHAVEN_CIVITAS_ILLA_FORTIS(
            PortLocation.BRIMHAVEN,
            PortLocation.CIVITAS_ILLA_FORTIS,
            // Sailing >= 38, used in 1 task
            new RelativeMove(0, 18),
            new RelativeMove(-5, 5),
            new RelativeMove(-34, 0),
            new RelativeMove(-27, -27),
            new RelativeMove(0, -83),
            new RelativeMove(32, -32),
            new RelativeMove(0, -283),
            new RelativeMove(-145, -145),
            new RelativeMove(-411, 0),
            new RelativeMove(-92, 92),
            new RelativeMove(-68, 0),
            new RelativeMove(-24, 24),
            new RelativeMove(0, 82),
            new RelativeMove(-51, 51),
            new RelativeMove(0, 172),
            new RelativeMove(-97, 97),
            new RelativeMove(-61, 0),
            new RelativeMove(-10, -10),
            new RelativeMove(0, -44),
            new RelativeMove(4, -4)
    ),
    ARDOUGNE_SUMMER_SHORE(
            PortLocation.ARDOUGNE,
            PortLocation.SUMMER_SHORE,
            // Sailing >= 45, used in 1 task
            new RelativeMove(8, 0),
            new RelativeMove(10, -10),
            new RelativeMove(0, -111),
            new RelativeMove(32, -32),
            new RelativeMove(0, -202),
            new RelativeMove(104, -104),
            new RelativeMove(0, -72),
            new RelativeMove(120, -120),
            new RelativeMove(0, -36),
            new RelativeMove(141, -141),
            new RelativeMove(0, -31),
            new RelativeMove(33, -33)
    ),
    ARDOUGNE_CIVITAS_ILLA_FORTIS(
            PortLocation.ARDOUGNE,
            PortLocation.CIVITAS_ILLA_FORTIS,
            // Sailing >= 38, used in 1 task
            new RelativeMove(7, 0),
            new RelativeMove(12, -12),
            new RelativeMove(0, -101),
            new RelativeMove(33, -33),
            new RelativeMove(0, -290),
            new RelativeMove(-138, -138),
            new RelativeMove(-415, 0),
            new RelativeMove(-82, 82),
            new RelativeMove(-78, 0),
            new RelativeMove(-27, 27),
            new RelativeMove(0, 91),
            new RelativeMove(-61, 61),
            new RelativeMove(0, 165),
            new RelativeMove(-93, 93),
            new RelativeMove(-54, 0),
            new RelativeMove(-13, -13),
            new RelativeMove(0, -43),
            new RelativeMove(4, -4)
    ),
    ARDOUGNE_VOID_KNIGHTS_OUTPOST(
            PortLocation.ARDOUGNE,
            PortLocation.VOID_KNIGHTS_OUTPOST,
            // Sailing >= 50, used in 1 task
            new RelativeMove(7, 0),
            new RelativeMove(11, -11),
            new RelativeMove(0, -109),
            new RelativeMove(33, -33),
            new RelativeMove(0, -255),
            new RelativeMove(-70, -70)
    ),
    PORT_KHAZARD_RELLEKKA(
            PortLocation.PORT_KHAZARD,
            PortLocation.RELLEKKA,
            // Sailing >= 62, used in 1 task
            new RelativeMove(0, -19),
            new RelativeMove(37, -37),
            new RelativeMove(0, -270),
            new RelativeMove(-151, -151),
            new RelativeMove(-403, 0),
            new RelativeMove(-96, 96),
            new RelativeMove(-60, 0),
            new RelativeMove(-34, 34),
            new RelativeMove(0, 109),
            new RelativeMove(35, 35),
            new RelativeMove(0, 142),
            new RelativeMove(-56, 56),
            new RelativeMove(0, 411),
            new RelativeMove(41, 41),
            new RelativeMove(177, 0),
            new RelativeMove(53, 53),
            new RelativeMove(0, 75),
            new RelativeMove(19, 19),
            new RelativeMove(216, 0),
            new RelativeMove(47, -47)
    ),
    PORT_KHAZARD_PORT_PISCARILIUS(
            PortLocation.PORT_KHAZARD,
            PortLocation.PORT_PISCARILIUS,
            // Sailing >= 30, used in 1 task
            new RelativeMove(0, -18),
            new RelativeMove(37, -37),
            new RelativeMove(0, -268),
            new RelativeMove(-155, -155),
            new RelativeMove(-401, 0),
            new RelativeMove(-78, 78),
            new RelativeMove(-68, 0),
            new RelativeMove(-44, 44),
            new RelativeMove(0, 73),
            new RelativeMove(-48, 48),
            new RelativeMove(0, 199),
            new RelativeMove(23, 23),
            new RelativeMove(0, 400),
            new RelativeMove(-109, 109)
    ),
    CORSAIR_COVE_CIVITAS_ILLA_FOTRIS(
            PortLocation.CORSAIR_COVE,
            PortLocation.CIVITAS_ILLA_FORTIS,
            // Sailing >= 40, used in 1 task
            new RelativeMove(48, -48),
            new RelativeMove(0, -54),
            new RelativeMove(-57, -57),
            new RelativeMove(-410, 0),
            new RelativeMove(-65, 65),
            new RelativeMove(-84, 0),
            new RelativeMove(-37, 37),
            new RelativeMove(0, 95),
            new RelativeMove(-72, 72),
            new RelativeMove(0, 171),
            new RelativeMove(-78, 78),
            new RelativeMove(-59, 0),
            new RelativeMove(-11, -11),
            new RelativeMove(0, -43),
            new RelativeMove(5, -5)
    ),
    CORSAIR_COVE_ALDARIN(
            PortLocation.CORSAIR_COVE,
            PortLocation.ALDARIN,
            // Sailing >= 46, used in 1 task
            new RelativeMove(45, -45),
            new RelativeMove(0, -54),
            new RelativeMove(-60, -60),
            new RelativeMove(-410, 0),
            new RelativeMove(-76, 76),
            new RelativeMove(-33, 0),
            new RelativeMove(-90, 90),
            new RelativeMove(-191, 0),
            new RelativeMove(-10, -10),
            new RelativeMove(-49, 0),
            new RelativeMove(-39, 39),
            new RelativeMove(-77, 0),
            new RelativeMove(-21, 21),
            new RelativeMove(0, 26),
            new RelativeMove(-10, 10),
            new RelativeMove(-47, 0),
            new RelativeMove(-40, 40)
    ),
    DEEPFIN_POINT_RUINS_OF_UNKAH(
            PortLocation.DEEPFIN_POINT,
            PortLocation.RUINS_OF_UNKAH,
            // Sailing >= 67, used in 1 task
            new RelativeMove(5, -5),
            new RelativeMove(172, 0),
            new RelativeMove(61, -61),
            new RelativeMove(587, 0),
            new RelativeMove(89, -89),
            new RelativeMove(249, 0),
            new RelativeMove(59, 59),
            new RelativeMove(0, 82),
            new RelativeMove(-9, 9),
            new RelativeMove(0, 65),
            new RelativeMove(7, 7)
    ),
    DEEPFIN_POINT_LANDS_END(
            PortLocation.DEEPFIN_POINT,
            PortLocation.LANDS_END,
            // Sailing >= 67, used in 1 task
            new RelativeMove(-34, 34),
            new RelativeMove(0, 156),
            new RelativeMove(37, 37),
            new RelativeMove(0, 145),
            new RelativeMove(29, 29),
            new RelativeMove(0, 191),
            new RelativeMove(-55, 55),
            new RelativeMove(-371, 0),
            new RelativeMove(-6, 6)
    ),
    LANDS_END_PORT_KHAZARD(
            PortLocation.LANDS_END,
            PortLocation.PORT_KHAZARD,
            // Sailing >= 30, used in 1 task
            new RelativeMove(0, -10),
            new RelativeMove(12, -12),
            new RelativeMove(134, 0),
            new RelativeMove(116, -116),
            new RelativeMove(150, 0),
            new RelativeMove(31, -31),
            new RelativeMove(0, -91),
            new RelativeMove(68, -68),
            new RelativeMove(0, -152),
            new RelativeMove(-42, -42),
            new RelativeMove(0, -77),
            new RelativeMove(50, -50),
            new RelativeMove(65, 0),
            new RelativeMove(132, -132),
            new RelativeMove(283, 0),
            new RelativeMove(215, 215),
            new RelativeMove(0, 272),
            new RelativeMove(-37, 37)
    ),
    LANDS_END_LUNAR_ISLE(
            PortLocation.LANDS_END,
            PortLocation.LUNAR_ISLE,
            // Sailing >= 76, used in 2 tasks
            new RelativeMove(8, -8),
            new RelativeMove(291, 0),
            new RelativeMove(113, 113),
            new RelativeMove(0, 140),
            new RelativeMove(143, 143),
            new RelativeMove(0, 6),
            new RelativeMove(43, 43),
            new RelativeMove(41, 0),
            new RelativeMove(6, 6),
            new RelativeMove(7, 0),
            new RelativeMove(9, 9),
            new RelativeMove(0, 11),
            new RelativeMove(-13, 13)
    ),
    MUSA_POINT_PORT_PISCARILIUS(
            PortLocation.MUSA_POINT,
            PortLocation.PORT_PISCARILIUS,
            // Sailing >= 15, used in 1 task
            new RelativeMove(0, -26),
            new RelativeMove(42, -42),
            new RelativeMove(0, -161),
            new RelativeMove(-38, -38),
            new RelativeMove(-201, 0),
            new RelativeMove(-192, -192),
            new RelativeMove(-418, 0),
            new RelativeMove(-85, 85),
            new RelativeMove(-60, 0),
            new RelativeMove(-32, 32),
            new RelativeMove(0, 78),
            new RelativeMove(-50, 50),
            new RelativeMove(0, 197),
            new RelativeMove(27, 27),
            new RelativeMove(0, 383),
            new RelativeMove(-113, 113)
    ),
    CIVITAS_ILLA_FORTIS_PORT_SARIM(
            PortLocation.CIVITAS_ILLA_FORTIS,
            PortLocation.PORT_SARIM,
            // Sailing >= 38, used in 1 task
            new RelativeMove(-8, 8),
            new RelativeMove(0, 39),
            new RelativeMove(15, 15),
            new RelativeMove(51, 0),
            new RelativeMove(92, -92),
            new RelativeMove(0, -177),
            new RelativeMove(63, -63),
            new RelativeMove(0, -59),
            new RelativeMove(62, -62),
            new RelativeMove(55, 0),
            new RelativeMove(69, -69),
            new RelativeMove(412, 0),
            new RelativeMove(196, 196),
            new RelativeMove(203, 0),
            new RelativeMove(27, 27),
            new RelativeMove(0, 183),
            new RelativeMove(50, 50)
    ),
    BRIMHAVEN_PORT_TYRAS(
            PortLocation.BRIMHAVEN,
            PortLocation.PORT_TYRAS,
            // Sailing >= 66, used in 1 task
            new RelativeMove(0, 15),
            new RelativeMove(-8, 8),
            new RelativeMove(-27, 0),
            new RelativeMove(-31, -31),
            new RelativeMove(0, -80),
            new RelativeMove(38, -38),
            new RelativeMove(0, -209),
            new RelativeMove(-85, -85),
            new RelativeMove(0, -72),
            new RelativeMove(-55, -55),
            new RelativeMove(-420, 0),
            new RelativeMove(-74, 74),
            new RelativeMove(-65, 0),
            new RelativeMove(-40, 40),
            new RelativeMove(0, 110),
            new RelativeMove(154, 154)
    ),
    LUNAR_ISLE_PORT_TYRAS(
            PortLocation.LUNAR_ISLE,
            PortLocation.PORT_TYRAS,
            // Sailing >= 76, used in 1 task
            new RelativeMove(2, 0),
            new RelativeMove(14, -14),
            new RelativeMove(0, -9),
            new RelativeMove(-5, -5),
            new RelativeMove(0, -52),
            new RelativeMove(41, -41),
            new RelativeMove(0, -129),
            new RelativeMove(-26, -26),
            new RelativeMove(-78, 0),
            new RelativeMove(-41, -41),
            new RelativeMove(0, -192),
            new RelativeMove(80, -80),
            new RelativeMove(0, -144),
            new RelativeMove(-13, -13),
            new RelativeMove(0, -14),
            new RelativeMove(6, -6)
    ),
    PORT_TYRAS_RED_ROCK(
            PortLocation.PORT_TYRAS,
            PortLocation.RED_ROCK,
            // Sailing >= 66, used in 1 task
            new RelativeMove(-21, -21),
            new RelativeMove(0, -67),
            new RelativeMove(-141, -141),
            new RelativeMove(0, -76),
            new RelativeMove(57, -57),
            new RelativeMove(68, 0),
            new RelativeMove(69, -69),
            new RelativeMove(311, 0),
            new RelativeMove(192, -192),
            new RelativeMove(136, 0),
            new RelativeMove(2, 2)
    ),
    PRIFDDINAS_RELLEKKA(
            PortLocation.PRIFDDINAS,
            PortLocation.RELLEKKA,
            // Sailing >= 70, used in 1 task
            new RelativeMove(-43, 0),
            new RelativeMove(-50, 50),
            new RelativeMove(0, 193),
            new RelativeMove(42, 42),
            new RelativeMove(78, 0),
            new RelativeMove(44, 44),
            new RelativeMove(0, 85),
            new RelativeMove(24, 24),
            new RelativeMove(211, 0),
            new RelativeMove(48, -48)
    ),
    PRIFDDINAS_VOID_KNIGHTS_OUTPOST(
            PortLocation.PRIFDDINAS,
            PortLocation.VOID_KNIGHTS_OUTPOST,
            // Sailing >= 70, used in 1 task
            new RelativeMove(-46, 0),
            new RelativeMove(-32, 32),
            new RelativeMove(-50, 0),
            new RelativeMove(-72, -72),
            new RelativeMove(0, -134),
            new RelativeMove(51, -51),
            new RelativeMove(0, -180),
            new RelativeMove(-27, -27),
            new RelativeMove(0, -78),
            new RelativeMove(47, -47),
            new RelativeMove(66, 0),
            new RelativeMove(79, -79)
    ),
    DEEPFIN_POINT_RELLEKKA(
            PortLocation.DEEPFIN_POINT,
            PortLocation.RELLEKKA,
            // Sailing >= 67, used in 1 task
            new RelativeMove(-24, 24),
            new RelativeMove(0, 182),
            new RelativeMove(33, 33),
            new RelativeMove(0, 138),
            new RelativeMove(26, 26),
            new RelativeMove(0, 404),
            new RelativeMove(49, 49),
            new RelativeMove(175, 0),
            new RelativeMove(58, 58),
            new RelativeMove(0, 78),
            new RelativeMove(14, 14),
            new RelativeMove(210, 0),
            new RelativeMove(49, -49)
    ),
    DEEPFIN_POINT_ETCETERIA(
            PortLocation.DEEPFIN_POINT,
            PortLocation.ETCETERIA,
            // Sailing >= 67, used in 1 task
            new RelativeMove(-24, 24),
            new RelativeMove(0, 182),
            new RelativeMove(33, 33),
            new RelativeMove(0, 138),
            new RelativeMove(26, 26),
            new RelativeMove(0, 404),
            new RelativeMove(49, 49),
            new RelativeMove(175, 0),
            new RelativeMove(58, 58),
            new RelativeMove(0, 78),
            new RelativeMove(14, 14),
            new RelativeMove(196, 0),
            new RelativeMove(39, 39),
            new RelativeMove(91, 0),
            new RelativeMove(32, 32)
    ),
    DEEPFIN_POINT_HOSIDIUS(
            PortLocation.DEEPFIN_POINT,
            PortLocation.HOSIDIUS,
            new RelativeMove(-32, 32),
            new RelativeMove(0, 152),
            new RelativeMove(33, 33),
            new RelativeMove(0, 149),
            new RelativeMove(33, 33),
            new RelativeMove(0, 183),
            new RelativeMove(-66, 66),
            new RelativeMove(-142, 0),
            new RelativeMove(-23, 23)
    ),
    RED_ROCK_DEEPFIN_POINT(
            PortLocation.RED_ROCK,
            PortLocation.DEEPFIN_POINT,
            new RelativeMove(0, -16),
            new RelativeMove(-9, -9),
            new RelativeMove(-330, 0),
            new RelativeMove(-122, 122),
            new RelativeMove(-49, 0),
            new RelativeMove(-80, 80),
            new RelativeMove(-249, 0),
            new RelativeMove(-52, 52)
    ),
    PORT_PISCARILIUS_DEEPFIN_POINT(
            PortLocation.PORT_PISCARILIUS,
            PortLocation.DEEPFIN_POINT,
            new RelativeMove(0, -23),
            new RelativeMove(106, -106),
            new RelativeMove(0, -414),
            new RelativeMove(-22, -22),
            new RelativeMove(0, -214),
            new RelativeMove(-16, -16),
            new RelativeMove(0, -126),
            new RelativeMove(8, -8)
    ),
    DEEPFIN_POINT_PORT_ROBERTS(
            PortLocation.DEEPFIN_POINT,
            PortLocation.PORT_ROBERTS,
            new RelativeMove(-44, 44),
            new RelativeMove(0, 103),
            new RelativeMove(42, 42),
            new RelativeMove(0, 176),
            new RelativeMove(32, 32),
            new RelativeMove(0, 83),
            new RelativeMove(-33, 33),
            new RelativeMove(-49, 0),
            new RelativeMove(-13, 13)
    ),
    ARDOUGNE_PORT_ROBERTS(
            PortLocation.ARDOUGNE,
            PortLocation.PORT_ROBERTS,
            new RelativeMove(19, -19),
            new RelativeMove(0, -93),
            new RelativeMove(36, -36),
            new RelativeMove(0, -273),
            new RelativeMove(-153, -153),
            new RelativeMove(-403, 0),
            new RelativeMove(-74, 74),
            new RelativeMove(-83, 0),
            new RelativeMove(-33, 33),
            new RelativeMove(0, 107),
            new RelativeMove(-47, 47),
            new RelativeMove(0, 177),
            new RelativeMove(24, 24),
            new RelativeMove(0, 80),
            new RelativeMove(-41, 41),
            new RelativeMove(-45, 0),
            new RelativeMove(-12, 12)
    ),
    PORT_ROBERTS_RED_ROCK(
            PortLocation.PORT_ROBERTS,
            PortLocation.RED_ROCK,
            new RelativeMove(0, -25),
            new RelativeMove(13, -13),
            new RelativeMove(48, 0),
            new RelativeMove(45, -45),
            new RelativeMove(0, -91),
            new RelativeMove(43, -43),
            new RelativeMove(0, -174),
            new RelativeMove(-29, -29),
            new RelativeMove(0, -79),
            new RelativeMove(54, -54),
            new RelativeMove(66, 0),
            new RelativeMove(66, -66),
            new RelativeMove(506, 0),
            new RelativeMove(72, -72),
            new RelativeMove(0, -85),
            new RelativeMove(39, -39),
            new RelativeMove(29, 0),
            new RelativeMove(4, 4)
    ),
    PORT_ROBERTS_PORT_SARIM(
            PortLocation.PORT_ROBERTS,
            PortLocation.PORT_SARIM,
            new RelativeMove(0, -22),
            new RelativeMove(17, -17),
            new RelativeMove(54, 0),
            new RelativeMove(28, -28),
            new RelativeMove(0, -100),
            new RelativeMove(44, -44),
            new RelativeMove(0, -183),
            new RelativeMove(-20, -20),
            new RelativeMove(0, -75),
            new RelativeMove(64, -64),
            new RelativeMove(49, 0),
            new RelativeMove(66, -66),
            new RelativeMove(422, 0),
            new RelativeMove(192, 192),
            new RelativeMove(200, 0),
            new RelativeMove(33, 33),
            new RelativeMove(0, 194),
            new RelativeMove(49, 49)
    ),
    PORT_ROBERTS_CATHERBY(
            PortLocation.PORT_ROBERTS,
            PortLocation.CATHERBY,
            new RelativeMove(0, -22),
            new RelativeMove(17, -17),
            new RelativeMove(40, 0),
            new RelativeMove(44, -44),
            new RelativeMove(0, -87),
            new RelativeMove(47, -47),
            new RelativeMove(0, -166),
            new RelativeMove(-26, -26),
            new RelativeMove(0, -95),
            new RelativeMove(50, -50),
            new RelativeMove(70, 0),
            new RelativeMove(68, -68),
            new RelativeMove(413, 0),
            new RelativeMove(144, 144),
            new RelativeMove(0, 280),
            new RelativeMove(-36, 36),
            new RelativeMove(0, 74),
            new RelativeMove(68, 68),
            new RelativeMove(0, 67),
            new RelativeMove(39, 39)
    ),
    PORT_ROBERTS_LANDS_END(
            PortLocation.PORT_ROBERTS,
            PortLocation.LANDS_END,
            new RelativeMove(-121, 0),
            new RelativeMove(-98, 98)
    ),
    ALDARIN_PISCATORIS(
            PortLocation.ALDARIN,
            PortLocation.PISCATORIS,
            new RelativeMove(20, 0),
            new RelativeMove(40, -40),
            new RelativeMove(51, 0),
            new RelativeMove(11, -11),
            new RelativeMove(0, -25),
            new RelativeMove(21, -21),
            new RelativeMove(137, 0),
            new RelativeMove(87, 87),
            new RelativeMove(0, 64),
            new RelativeMove(137, 137),
            new RelativeMove(0, 401),
            new RelativeMove(36, 36),
            new RelativeMove(193, 0),
            new RelativeMove(59, 59),
            new RelativeMove(38, 0),
            new RelativeMove(16, 16)
    ),
    ALDARIN_RELLEKKA(
            PortLocation.ALDARIN,
            PortLocation.RELLEKKA,
            new RelativeMove(20, 0),
            new RelativeMove(40, -40),
            new RelativeMove(51, 0),
            new RelativeMove(11, -11),
            new RelativeMove(0, -25),
            new RelativeMove(21, -21),
            new RelativeMove(137, 0),
            new RelativeMove(87, 87),
            new RelativeMove(0, 64),
            new RelativeMove(137, 137),
            new RelativeMove(0, 401),
            new RelativeMove(36, 36),
            new RelativeMove(193, 0),
            new RelativeMove(35, 35),
            new RelativeMove(0, 100),
            new RelativeMove(16, 16),
            new RelativeMove(227, 0),
            new RelativeMove(47, -47)
    ),
    ALDARIN_BRIMHAVEN(
            PortLocation.ALDARIN,
            PortLocation.BRIMHAVEN,
            new RelativeMove(20, 0),
            new RelativeMove(40, -40),
            new RelativeMove(51, 0),
            new RelativeMove(11, -11),
            new RelativeMove(0, -25),
            new RelativeMove(21, -21),
            new RelativeMove(117, 0),
            new RelativeMove(195, -195),
            new RelativeMove(668, 0),
            new RelativeMove(148, 148),
            new RelativeMove(0, 284),
            new RelativeMove(-36, 36),
            new RelativeMove(0, 74),
            new RelativeMove(28, 28),
            new RelativeMove(33, 0),
            new RelativeMove(4, -4)
    ),
    ALDARIN_VOID_KNIGHTS_OUTPOST(
            PortLocation.ALDARIN,
            PortLocation.VOID_KNIGHTS_OUTPOST,
            new RelativeMove(20, 0),
            new RelativeMove(40, -40),
            new RelativeMove(51, 0),
            new RelativeMove(11, -11),
            new RelativeMove(0, -25),
            new RelativeMove(21, -21),
            new RelativeMove(117, 0),
            new RelativeMove(197, -197)
    ),
    ALDARIN_PORT_ROBERTS(
            PortLocation.ALDARIN,
            PortLocation.PORT_ROBERTS,
            new RelativeMove(20, 0),
            new RelativeMove(40, -40),
            new RelativeMove(51, 0),
            new RelativeMove(11, -11),
            new RelativeMove(0, -25),
            new RelativeMove(21, -21),
            new RelativeMove(134, 0),
            new RelativeMove(88, 88),
            new RelativeMove(0, 62),
            new RelativeMove(72, 72),
            new RelativeMove(0, 42),
            new RelativeMove(-33, 33)
    ),
    ALDARIN_SUNSET_COAST(
            PortLocation.ALDARIN,
            PortLocation.SUNSET_COAST,
            new RelativeMove(22, 0),
            new RelativeMove(6, -6)
    ),
    ALDARIN_DEEPFIN_POINT(
            PortLocation.ALDARIN,
            PortLocation.DEEPFIN_POINT,
            new RelativeMove(20, 0),
            new RelativeMove(40, -40),
            new RelativeMove(51, 0),
            new RelativeMove(11, -11),
            new RelativeMove(0, -25),
            new RelativeMove(21, -21),
            new RelativeMove(115, 0),
            new RelativeMove(128, -128)
    ),
    RED_ROCK_CIVITAS_ILLA_FORTIS(
            PortLocation.RED_ROCK,
            PortLocation.CIVITAS_ILLA_FORTIS,
            new RelativeMove(0, -16),
            new RelativeMove(-9, -9),
            new RelativeMove(-336, 0),
            new RelativeMove(-124, 124),
            new RelativeMove(-95, 0),
            new RelativeMove(-172, 172),
            new RelativeMove(-62, 0),
            new RelativeMove(-36, 36),
            new RelativeMove(0, 60),
            new RelativeMove(-68, 68),
            new RelativeMove(0, 177),
            new RelativeMove(-82, 82),
            new RelativeMove(-58, 0),
            new RelativeMove(-11, -11),
            new RelativeMove(0, -45),
            new RelativeMove(4, -4)
    ),
    CIVITAS_ILLA_FORTIS_SUMMER_SHORE(
            PortLocation.CIVITAS_ILLA_FORTIS,
            PortLocation.SUMMER_SHORE,
            new RelativeMove(-9, 9),
            new RelativeMove(0, 40),
            new RelativeMove(17, 17),
            new RelativeMove(48, 0),
            new RelativeMove(107, -107),
            new RelativeMove(0, -166),
            new RelativeMove(51, -51),
            new RelativeMove(0, -68),
            new RelativeMove(61, -61),
            new RelativeMove(72, 0),
            new RelativeMove(24, -24),
            new RelativeMove(0, -26),
            new RelativeMove(21, -21),
            new RelativeMove(706, 0),
            new RelativeMove(69, -69),
            new RelativeMove(0, -43),
            new RelativeMove(148, -148),
            new RelativeMove(0, -25),
            new RelativeMove(34, -34)
    ),
    CIVITAS_ILLA_FORTIS_VOID_KNIGHTS_OUTPOST(
            PortLocation.CIVITAS_ILLA_FORTIS,
            PortLocation.VOID_KNIGHTS_OUTPOST,
            new RelativeMove(-9, 9),
            new RelativeMove(0, 40),
            new RelativeMove(17, 17),
            new RelativeMove(48, 0),
            new RelativeMove(107, -107),
            new RelativeMove(0, -166),
            new RelativeMove(51, -51),
            new RelativeMove(0, -68),
            new RelativeMove(61, -61),
            new RelativeMove(72, 0),
            new RelativeMove(24, -24),
            new RelativeMove(0, -26),
            new RelativeMove(24, -24)
    ),
    PORT_ROBERTS_VOID_KNIGHTS_OUTPOST(
            PortLocation.PORT_ROBERTS,
            PortLocation.VOID_KNIGHTS_OUTPOST,
            new RelativeMove(0, -24),
            new RelativeMove(15, -15),
            new RelativeMove(48, 0),
            new RelativeMove(38, -38),
            new RelativeMove(0, -83),
            new RelativeMove(49, -49),
            new RelativeMove(0, -165),
            new RelativeMove(-24, -24),
            new RelativeMove(0, -105),
            new RelativeMove(47, -47),
            new RelativeMove(63, 0),
            new RelativeMove(74, -74)
    ),
    VOID_KNIGHTS_OUTPOST_PORT_KHAZARD(
            PortLocation.VOID_KNIGHTS_OUTPOST,
            PortLocation.PORT_KHAZARD,
            new RelativeMove(0, 114),
            new RelativeMove(74, 74),
            new RelativeMove(0, 237),
            new RelativeMove(-37, 37)
    ),
    VOID_KNIGHTS_OUTPOST_PORT_TYRAS(
            PortLocation.VOID_KNIGHTS_OUTPOST,
            PortLocation.PORT_TYRAS,
            new RelativeMove(-478, 0),
            new RelativeMove(-78, 78),
            new RelativeMove(-54, 0),
            new RelativeMove(-58, 58),
            new RelativeMove(0, 69),
            new RelativeMove(158, 158)
    ),
    SUMMER_SHORE_PANDEMONIUM(
            PortLocation.SUMMER_SHORE,
            PortLocation.PANDEMONIUM,
            new RelativeMove(-64, 0),
            new RelativeMove(-42, 42),
            new RelativeMove(0, 261),
            new RelativeMove(-33, 33),
            new RelativeMove(0, 67),
            new RelativeMove(30, 30),
            new RelativeMove(0, 139),
            new RelativeMove(13, 13)
    ),
    SUMMER_SHORE_PORT_SARIM(
            PortLocation.SUMMER_SHORE,
            PortLocation.PORT_SARIM,
            new RelativeMove(-64, 0),
            new RelativeMove(-42, 42),
            new RelativeMove(0, 261),
            new RelativeMove(-47, 47),
            new RelativeMove(0, 53),
            new RelativeMove(-52, 52),
            new RelativeMove(0, 35),
            new RelativeMove(38, 38),
            new RelativeMove(0, 193),
            new RelativeMove(49, 49)
    ),
    SUMMER_SHORE_ALDARIN(
            PortLocation.SUMMER_SHORE,
            PortLocation.ALDARIN,
            new RelativeMove(-158, 0),
            new RelativeMove(-117, 117),
            new RelativeMove(-255, 0),
            new RelativeMove(-131, 131),
            new RelativeMove(-91, 0),
            new RelativeMove(-71, 71),
            new RelativeMove(-465, 0),
            new RelativeMove(-194, 194),
            new RelativeMove(-95, 0),
            new RelativeMove(-22, 22),
            new RelativeMove(0, 26),
            new RelativeMove(-8, 8),
            new RelativeMove(-49, 0),
            new RelativeMove(-41, 41)
    ),
    SUMMER_SHORE_PORT_ROBERTS(
            PortLocation.SUMMER_SHORE,
            PortLocation.PORT_ROBERTS,
            new RelativeMove(-158, 0),
            new RelativeMove(-117, 117),
            new RelativeMove(-255, 0),
            new RelativeMove(-131, 131),
            new RelativeMove(-91, 0),
            new RelativeMove(-74, 74),
            new RelativeMove(-189, 0),
            new RelativeMove(-93, 93),
            new RelativeMove(-51, 0),
            new RelativeMove(-38, 38),
            new RelativeMove(0, 99),
            new RelativeMove(28, 28),
            new RelativeMove(0, 170),
            new RelativeMove(-46, 46),
            new RelativeMove(0, 77),
            new RelativeMove(-27, 27),
            new RelativeMove(-65, 0),
            new RelativeMove(-9, 9)
    ),
    SUMMER_SHORE_DEEPFIN_POINT(
            PortLocation.SUMMER_SHORE,
            PortLocation.DEEPFIN_POINT,
            new RelativeMove(-158, 0),
            new RelativeMove(-117, 117),
            new RelativeMove(-255, 0),
            new RelativeMove(-131, 131),
            new RelativeMove(-91, 0),
            new RelativeMove(-72, 72),
            new RelativeMove(-371, 0),
            new RelativeMove(-56, 56)
    ),
    SUMMER_SHORE_PORT_KHAZARD(
            PortLocation.SUMMER_SHORE,
            PortLocation.PORT_KHAZARD,
            new RelativeMove(-64, 0),
            new RelativeMove(-38, 38),
            new RelativeMove(-155, 0),
            new RelativeMove(-96, 96),
            new RelativeMove(0, 122),
            new RelativeMove(-2, 2),
            new RelativeMove(0, 185),
            new RelativeMove(-94, 94),
            new RelativeMove(0, 207),
            new RelativeMove(-37, 37)
    ),
    RED_ROCK_PORT_KHAZARD(
            PortLocation.RED_ROCK,
            PortLocation.PORT_KHAZARD,
            new RelativeMove(17, 17),
            new RelativeMove(0, 71),
            new RelativeMove(-77, 77),
            new RelativeMove(-64, 0),
            new RelativeMove(-46, 46),
            new RelativeMove(0, 55),
            new RelativeMove(81, 81),
            new RelativeMove(0, 251),
            new RelativeMove(-37, 37)
    ),
    RED_ROCK_PORT_SARIM(
            PortLocation.RED_ROCK,
            PortLocation.PORT_SARIM,
            new RelativeMove(7, 7),
            new RelativeMove(0, 93),
            new RelativeMove(-2, 2),
            new RelativeMove(0, 227),
            new RelativeMove(41, 41),
            new RelativeMove(112, 0),
            new RelativeMove(34, 34),
            new RelativeMove(0, 193),
            new RelativeMove(50, 50)
    ),
    RED_ROCK_PORT_PISCARILIUS(
            PortLocation.RED_ROCK,
            PortLocation.PORT_PISCARILIUS,
            new RelativeMove(0, -17),
            new RelativeMove(-6, -6),
            new RelativeMove(-361, 0),
            new RelativeMove(-197, 197),
            new RelativeMove(-274, 0),
            new RelativeMove(-75, 75),
            new RelativeMove(0, 198),
            new RelativeMove(11, 11),
            new RelativeMove(0, 156),
            new RelativeMove(-56, 56),
            new RelativeMove(0, 143),
            new RelativeMove(52, 52),
            new RelativeMove(0, 216),
            new RelativeMove(-63, 63)
    ),
    RED_ROCK_LANDS_END(
            PortLocation.RED_ROCK,
            PortLocation.LANDS_END,
            new RelativeMove(0, -17),
            new RelativeMove(-6, -6),
            new RelativeMove(-361, 0),
            new RelativeMove(-197, 197),
            new RelativeMove(-274, 0),
            new RelativeMove(-75, 75),
            new RelativeMove(0, 198),
            new RelativeMove(11, 11),
            new RelativeMove(0, 156),
            new RelativeMove(-281, 281)
    ),
    RED_ROCK_CATHERBY(
            PortLocation.RED_ROCK,
            PortLocation.CATHERBY,
            new RelativeMove(7, 7),
            new RelativeMove(0, 103),
            new RelativeMove(-2, 2),
            new RelativeMove(0, 206),
            new RelativeMove(-94, 94),
            new RelativeMove(0, 203),
            new RelativeMove(-36, 36),
            new RelativeMove(0, 59),
            new RelativeMove(66, 66),
            new RelativeMove(0, 67),
            new RelativeMove(41, 41)
    ),
    LANDS_END_HOSIDIUS(
            PortLocation.LANDS_END,
            PortLocation.HOSIDIUS,
            new RelativeMove(9, -9),
            new RelativeMove(167, 0),
            new RelativeMove(39, 39)
    ),
    ;

    private final PortLocation start;
    private final PortLocation end;
    private final List<RelativeMove> pathPoints;
    private final double distance;

    PortPaths(PortLocation start, PortLocation end, RelativeMove... pathPoints) {
        this.start = start;
        this.end = end;
        this.pathPoints = List.of(pathPoints);
        this.distance = computeDistance();
    }

    public List<BoatPathing.Waypoint> getWaypointPath(boolean reverse)
    {
        List<WorldPoint> fullPath = getFullPath();
        if (reverse)
        {
            Collections.reverse(fullPath);
        }
        return BoatPathing.convertToWaypoints(fullPath);
    }

    public List<WorldPoint> getFullPath()
    {
        List<WorldPoint> fullPath = new ArrayList<>();
        WorldPoint current = start.getNavigationLocation();
        fullPath.add(current);
        for (RelativeMove delta : pathPoints)
        {
            List<RelativeMove> moves = splitMove(delta, 50); // List<RelativeMove> moves = List.of(delta); to remove segmentation
            for (RelativeMove m : moves)
            {
                current = new WorldPoint(current.getX() + m.getDx(), current.getY() + m.getDy(), current.getPlane());
                fullPath.add(current);
            }
        }
        fullPath.add(end.getNavigationLocation());
        return fullPath;
    }
    private List<RelativeMove> splitMove(RelativeMove delta, int segmentLength)
    {
        int dx = delta.getDx();
        int dy = delta.getDy();
        int steps = Math.max(
                Math.abs(dx) / segmentLength + (Math.abs(dx) % segmentLength != 0 ? 1 : 0),
                Math.abs(dy) / segmentLength + (Math.abs(dy) % segmentLength != 0 ? 1 : 0)
        );

        if (steps <= 1)
            return List.of(delta);

        List<RelativeMove> result = new ArrayList<>(steps);

        int baseDx = dx / steps;
        int baseDy = dy / steps;

        int usedAbsDx = Math.abs(baseDx) * steps;
        int usedAbsDy = Math.abs(baseDy) * steps;

        int remDx = Math.abs(dx) - usedAbsDx;
        int remDy = Math.abs(dy) - usedAbsDy;

        int signDx = Integer.signum(dx);
        int signDy = Integer.signum(dy);

        for (int i = 0; i < steps; i++)
        {
            int extraDx = i < remDx ? signDx : 0;
            int extraDy = i < remDy ? signDy : 0;

            int addX = baseDx + extraDx;
            int addY = baseDy + extraDy;

            result.add(new RelativeMove(addX, addY));
        }
        return result;
    }

    private double computeDistance()
    {
        double total = 0;

        WorldPoint current = start.getNavigationLocation();
        for (RelativeMove delta : pathPoints)
        {
            WorldPoint next = new WorldPoint(
                    current.getX() + delta.getDx(),
                    current.getY() + delta.getDy(),
                    current.getPlane()
            );

            total += distanceBetween(current, next);
            current = next;
        }
        WorldPoint endPoint = end.getNavigationLocation();
        if (!current.equals(endPoint))
        {
            total += distanceBetween(current, endPoint);
        }
        return total;
    }

    private static double distanceBetween(WorldPoint a, WorldPoint b)
    {
        int dx = b.getX() - a.getX();
        int dy = b.getY() - a.getY();
        return Math.hypot(dx, dy);
    }
    @Override
    public String toString()
    {
        return String.format("%s -> %s (%d points)", start.name(), end.name(), pathPoints.size());
    }

}