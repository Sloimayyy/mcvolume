package com.sloimay.mcvolume.utils

enum class McVersion(val dataVersion: Int) {
    JE_1_21_5(4325),
    JE_1_21_5_RC2(4324),
    JE_1_21_5_RC1(4323),
    JE_1_21_5_PRE3(4322),
    JE_1_21_5_PRE2(4321),
    JE_1_21_5_PRE1(4320),
    JE_25W10A(4319),
    JE_25W09B(4318),
    JE_25W09A(4317),
    JE_25W08A(4316),
    JE_25W07A(4315),
    JE_25W06A(4313),
    JE_25W05A(4310),
    JE_25W04A(4308),
    JE_25W03A(4304),
    JE_25W02A(4298),
    JE_1_21_4(4189),
    JE_1_21_4_RC3(4188),
    JE_1_21_4_RC2(4186),
    JE_1_21_4_RC1(4184),
    JE_1_21_4_PRE3(4183),
    JE_1_21_4_PRE2(4182),
    JE_1_21_4_PRE1(4179),
    JE_24W46A(4178),
    JE_24W45A(4177),
    JE_24W44A(4174),
    JE_1_21_3(4082),
    JE_1_21_2(4080),
    JE_1_21_2_RC2(4079),
    JE_1_21_2_RC1(4078),
    JE_1_21_2_PRE5(4077),
    JE_1_21_2_PRE4(4076),
    JE_1_21_2_PRE3(4075),
    JE_1_21_2_PRE2(4074),
    JE_1_21_2_PRE1(4073),
    JE_24W40A(4072),
    JE_24W39A(4069),
    JE_24W38A(4066),
    JE_24W37A(4065),
    JE_24W36A(4063),
    JE_24W35A(4062),
    JE_24W34A(4060),
    JE_24W33A(4058),
    JE_1_21_1(3955),
    JE_1_21_1_RC1(3954),
    JE_1_21(3953),
    JE_1_21_RC1(3952),
    JE_1_21_PRE4(3951),
    JE_1_21_PRE3(3950),
    JE_1_21_PRE2(3949),
    JE_1_21_PRE1(3948),
    JE_24W21B(3947),
    JE_24W21A(3946),
    JE_24W20A(3944),
    JE_24W19B(3942),
    JE_24W19A(3941),
    JE_24W18A(3940),
    JE_1_20_6(3839),
    JE_1_20_6_RC1(3838),
    JE_1_20_5(3837),
    JE_1_20_5_RC3(3836),
    JE_1_20_5_RC2(3835),
    JE_1_20_5_RC1(3834),
    JE_1_20_5_PRE4(3832),
    JE_1_20_5_PRE3(3831),
    JE_1_20_5_PRE2(3830),
    JE_1_20_5_PRE1(3829),
    JE_24W14A(3827),
    JE_24W14POTATO(3824),
    JE_24W13A(3826),
    JE_24W12A(3824),
    JE_24W11A(3823),
    JE_24W10A(3821),
    JE_24W09A(3819),
    JE_24W07A(3817),
    JE_24W06A(3815),
    JE_24W05B(3811),
    JE_24W05A(3809),
    JE_24W04A(3806),
    JE_24W03B(3805),
    JE_24W03A(3804),
    JE_23W51B(3802),
    JE_1_20_4(3700),
    JE_1_20_4_RC1(3699),
    JE_1_20_3(3698),
    JE_1_20_3_RC1(3697),
    JE_1_20_3_PRE4(3696),
    JE_1_20_3_PRE3(3695),
    JE_1_20_3_PRE2(3694),
    JE_1_20_3_PRE1(3693),
    JE_23W46A(3691),
    JE_23W45A(3690),
    JE_23W44A(3688),
    JE_23W43B(3687),
    JE_23W43A(3686),
    JE_23W42A(3684),
    JE_23W41A(3681),
    JE_23W40A(3679),
    JE_1_20_2(3578),
    JE_1_20_2_RC2(3577),
    JE_1_20_2_RC1(3576),
    JE_1_20_2_PRE4(3575),
    JE_1_20_2_PRE3(3574),
    JE_1_20_2_PRE2(3573),
    JE_1_20_2_PRE1(3572),
    JE_23W35A(3571),
    JE_23W33A(3570),
    JE_1_20_1(3465),
    JE_1_20(3463),
    JE_1_19_4(3337),
    JE_23W03A(3320),
    JE_1_19_3(3218),
    JE_1_19_3_RC3(3217),
    JE_1_19_3_RC2(3216),
    JE_1_19_3_RC1(3215),
    JE_1_19_3_PRE3(3213),
    JE_1_19_3_PRE2(3212),
    JE_1_19_3_PRE1(3211),
    JE_22W46A(3210),
    JE_22W45A(3208),
    JE_22W44A(3207),
    JE_22W43A(3206),
    JE_22W42A(3205),
    JE_1_19_2(3120),
    JE_1_19_2_RC2(3119),
    JE_1_19_2_RC1(3118),
    JE_1_19_1(3117),
    JE_1_19_1_RC3(3116),
    JE_1_19_1_RC2(3115),
    JE_1_19_1_PRE6(3114),
    JE_1_19_1_PRE5(3113),
    JE_1_19_1_PRE4(3112),
    JE_1_19_1_PRE3(3111),
    JE_1_19_1_PRE2(3110),
    JE_1_19_1_RC1(3109),
    JE_1_19_1_PRE1(3107),
    JE_22W24A(3106),
    JE_1_19(3105),
    JE_1_19_RC2(3104),
    JE_1_19_RC1(3103),
    JE_1_19_PRE5(3102),
    JE_1_19_PRE4(3101),
    JE_1_19_PRE3(3100),
    JE_1_19_PRE2(3099),
    JE_1_19_PRE1(3098),
    JE_22W19A(3096),
    JE_22W18A(3095),
    JE_22W17A(3093),
    JE_22W16B(3092),
    JE_22W16A(3091),
    JE_22W15A(3089),
    JE_22W14A(3088),
    JE_22W13ONEBLOCKATATIME(3076),
    JE_22W13A(3085),
    JE_22W12A(3082),
    JE_22W11A(3080),
    JE_1_18_2(2975),
    JE_1_18_2_RC1(2974),
    JE_1_18_2_PRE3(2973),
    JE_1_18_2_PRE2(2972),
    JE_1_18_2_PRE1(2971),
    JE_22W07A(2969),
    JE_22W06A(2968),
    JE_22W05A(2967),
    JE_22W03A(2966),
    JE_1_18_1(2865),
    JE_1_18_1_RC3(2864),
    JE_1_18_1_RC2(2863),
    JE_1_18_1_RC1(2862),
    JE_1_18_1_PRE1(2861),
    JE_1_18(2860),
    JE_1_18_RC4(2859),
    JE_1_18_RC3(2858),
    JE_1_18_RC2(2857),
    JE_1_18_RC1(2856),
    JE_1_18_PRE8(2855),
    JE_1_18_PRE7(2854),
    JE_1_18_PRE6(2853),
    JE_1_18_PRE5(2851),
    JE_1_18_PRE4(2850),
    JE_1_18_PRE3(2849),
    JE_1_18_PRE2(2848),
    JE_1_18_PRE1(2847),
    JE_21W44A(2845),
    JE_21W43A(2844),
    JE_21W42A(2840),
    JE_21W41A(2839),
    JE_21W40A(2838),
    JE_21W39A(2836),
    JE_21W38A(2835),
    JE_21W37A(2834),
    JE_1_17_1(2730),
    JE_1_17_1_RC2(2729),
    JE_1_17_1_RC1(2728),
    JE_1_17_1_PRE3(2727),
    JE_1_17_1_PRE2(2726),
    JE_1_17_1_PRE1(2725),
    JE_1_17(2724),
    JE_1_17_RC2(2723),
    JE_1_17_RC1(2722),
    JE_1_17_PRE5(2721),
    JE_1_17_PRE4(2720),
    JE_1_17_PRE3(2719),
    JE_1_17_PRE2(2718),
    JE_1_17_PRE1(2716),
    JE_21W20A(2715),
    JE_21W19A(2714),
    JE_21W18A(2713),
    JE_21W17A(2712),
    JE_21W16A(2711),
    JE_21W15A(2709),
    JE_21W14A(2706),
    JE_21W13A(2705),
    JE_21W11A(2703),
    JE_21W10A(2699),
    JE_21W08B(2698),
    JE_21W08A(2697),
    JE_21W07A(2695),
    JE_21W06A(2694),
    JE_21W05B(2692),
    JE_21W05A(2690),
    JE_21W03A(2689),
    JE_1_16_5(2586),
    JE_1_16_5_RC1(2585),
    JE_20W51A(2687),
    JE_20W49A(2685),
    JE_20W48A(2683),
    JE_20W46A(2682),
    JE_20W45A(2681),
    JE_1_16_4(2584),
    JE_1_16_4_RC1(2583),
    JE_1_16_4_PRE2(2582),
    JE_1_16_4_PRE1(2581),
    JE_1_16_3(2580),
    JE_1_16_3_RC1(2579),
    JE_1_16_2(2578),
    JE_1_16_2_RC2(2577),
    JE_1_16_2_RC1(2576),
    JE_1_16_2_PRE3(2575),
    JE_1_16_2_PRE2(2574),
    JE_1_16_2_PRE1(2573),
    JE_20W30A(2572),
    JE_20W29A(2571),
    JE_20W28A(2570),
    JE_20W27A(2569),
    JE_1_16_1(2567),
    JE_1_16(2566),
    JE_1_16_RC1(2565),
    JE_1_16_PRE8(2564),
    JE_1_16_PRE7(2563),
    JE_1_16_PRE6(2562),
    JE_1_16_PRE5(2561),
    JE_1_16_PRE4(2560),
    JE_1_16_PRE3(2559),
    JE_1_16_PRE2(2557),
    JE_1_16_PRE1(2556),
    JE_20W22A(2555),
    JE_20W21A(2554),
    JE_20W20B(2537),
    JE_20W20A(2536),
    JE_20W19A(2534),
    JE_20W18A(2532),
    JE_20W17A(2529),
    JE_20W16A(2526),
    JE_20W15A(2525),
    JE_20W14A(2524),
    JE_20W14INFINITE(2522),
    JE_20W13B(2521),
    JE_20W13A(2520),
    JE_20W12A(2515),
    JE_20W11A(2513),
    JE_20W10A(2512),
    JE_20W09A(2510),
    JE_20W08A(2507),
    JE_20W07A(2506),
    JE_20W06A(2504),
    JE_1_15_2(2230),
    JE_1_15_2_PRE2(2229),
    JE_1_15_2_PRE1(2228),
    JE_1_15_1(2227),
    JE_1_15_1_PRE1(2226),
    JE_1_15(2225),
    JE_1_15_PRE7(2224),
    JE_1_15_PRE6(2223),
    JE_1_15_PRE5(2222),
    JE_1_15_PRE4(2221),
    JE_1_15_PRE3(2220),
    JE_1_15_PRE2(2219),
    JE_1_15_PRE1(2218),
    JE_19W46B(2217),
    JE_19W46A(2216),
    JE_19W45B(2215),
    JE_19W45A(2214),
    JE_19W44A(2213),
    JE_19W42A(2212),
    JE_19W41A(2210),
    JE_19W40A(2208),
    JE_19W39A(2207),
    JE_19W38B(2206),
    JE_19W38A(2205),
    JE_19W37A(2204),
    JE_19W36A(2203),
    JE_19W35A(2201),
    JE_19W34A(2200),
    JE_1_14_4(1976),
    JE_1_14_4_PRE7(1975),
    JE_1_14_4_PRE6(1974),
    JE_1_14_4_PRE5(1973),
    JE_1_14_4_PRE4(1972),
    JE_1_14_4_PRE3(1971),
    JE_1_14_4_PRE2(1970),
    JE_1_14_4_PRE1(1969),
    JE_1_14_3(1968),
    JE_1_14_3_PRE4(1967),
    JE_1_14_3_PRE3(1966),
    JE_1_14_3_PRE2(1965),
    JE_1_14_3_PRE1(1964),
    JE_1_14_2(1963),
    JE_1_14_2_PRE4(1962),
    JE_1_14_2_PRE3(1960),
    JE_1_14_2_PRE2(1959),
    JE_1_14_2_PRE1(1958),
    JE_1_14_1(1957),
    JE_1_14_1_PRE2(1956),
    JE_1_14_1_PRE1(1955),
    JE_1_14(1952),
    JE_1_14_PRE5(1951),
    JE_1_14_PRE4(1950),
    JE_1_14_PRE3(1949),
    JE_1_14_PRE2(1948),
    JE_1_14_PRE1(1947),
    JE_19W14B(1945),
    JE_19W14A(1944),
    JE_19W13B(1943),
    JE_19W13A(1942),
    JE_19W12B(1941),
    JE_19W12A(1940),
    JE_19W11B(1938),
    JE_19W11A(1937),
    JE_19W09A(1935),
    JE_19W08B(1934),
    JE_19W08A(1933),
    JE_19W07A(1932),
    JE_19W06A(1931),
    JE_19W05A(1930),
    JE_19W04B(1927),
    JE_19W04A(1926),
    JE_19W03C(1924),
    JE_19W03B(1923),
    JE_19W03A(1922),
    JE_19W02A(1921),
    JE_18W50A(1919),
    JE_18W49A(1916),
    JE_18W48B(1915),
    JE_18W48A(1914),
    JE_18W47B(1913),
    JE_18W47A(1912),
    JE_18W46A(1910),
    JE_18W45A(1908),
    JE_18W44A(1907),
    JE_18W43C(1903),
    JE_18W43B(1902),
    JE_18W43A(1902),
    JE_1_13_2(1631),
    JE_1_13_2_PRE2(1630),
    JE_1_13_2_PRE1(1629),
    JE_1_13_1(1628),
    JE_1_13_1_PRE2(1627),
    JE_1_13_1_PRE1(1626),
    JE_18W33A(1625),
    JE_18W32A(1623),
    JE_18W31A(1622),
    JE_18W30B(1621),
    JE_18W30A(1620),
    JE_1_13(1519),
    JE_1_13_PRE10(1518),
    JE_1_13_PRE9(1517),
    JE_1_13_PRE8(1516),
    JE_1_13_PRE7(1513),
    JE_1_13_PRE6(1512),
    JE_1_13_PRE5(1511),
    JE_1_13_PRE4(1504),
    JE_1_13_PRE3(1503),
    JE_1_13_PRE2(1502),
    JE_1_13_PRE1(1501),
    JE_18W22C(1499),
    JE_18W22B(1498),
    JE_18W22A(1497),
    JE_18W21B(1496),
    JE_18W21A(1495),
    JE_18W20C(1493),
    JE_18W20B(1491),
    JE_18W20A(1489),
    JE_18W19B(1485),
    JE_18W19A(1484),
    JE_18W16A(1483),
    JE_18W15A(1482),
    JE_18W14B(1481),
    JE_18W14A(1479),
    JE_18W11A(1478),
    JE_18W10D(1477),
    JE_18W10C(1476),
    JE_18W10B(1474),
    JE_18W10A(1473),
    JE_18W09A(1472),
    JE_18W08B(1471),
    JE_18W08A(1470),
    JE_18W07C(1469),
    JE_18W07B(1468),
    JE_18W07A(1467),
    JE_18W06A(1466),
    JE_18W05A(1464),
    JE_18W03B(1463),
    JE_18W03A(1462),
    JE_18W02A(1461),
    JE_18W01A(1459),
    JE_17W50A(1457),
    JE_17W49B(1455),
    JE_17W49A(1454),
    JE_17W48A(1453),
    JE_17W47B(1452),
    JE_17W47A(1451),
    JE_17W46A(1449),
    JE_17W45B(1448),
    JE_17W45A(1447),
    JE_17W43B(1445),
    JE_17W43A(1444),
    JE_1_12_2(1343),
    JE_1_12_2_PRE2(1342),
    JE_1_12_2_PRE1(1341),
    JE_1_12_1(1241),
    JE_1_12_1_PRE1(1240),
    JE_17W31A(1239),
    JE_1_12(1139),
    JE_1_12_PRE7(1138),
    JE_1_12_PRE6(1137),
    JE_1_12_PRE5(1136),
    JE_1_12_PRE4(1135),
    JE_1_12_PRE3(1134),
    JE_1_12_PRE2(1133),
    JE_1_12_PRE1(1132),
    JE_17W18B(1131),
    JE_17W18A(1130),
    JE_17W17B(1129),
    JE_17W17A(1128),
    JE_17W16B(1127),
    JE_17W16A(1126),
    JE_17W15A(1125),
    JE_17W14A(1124),
    JE_17W13B(1123),
    JE_17W13A(1122),
    JE_17W06A(1022),
    JE_1_11_2(922),
    JE_1_11_1(921),
    JE_16W50A(920),
    JE_1_11(819),
    JE_1_11_PRE1(818),
    JE_16W44A(817),
    JE_16W43A(817),
    JE_16W42A(815),
    JE_16W41A(814),
    JE_16W40A(813),
    JE_16W39C(812),
    JE_16W39B(811),
    JE_16W39A(809),
    JE_16W38A(807),
    JE_16W36A(805),
    JE_16W35A(803),
    JE_16W33A(802),
    JE_16W32B(801),
    JE_16W32A(800),
    JE_1_10_2(512),
    JE_1_10_1(511),
    JE_1_10(510),
    JE_1_10_PRE2(507),
    JE_1_10_PRE1(506),
    JE_16W21B(504),
    JE_16W21A(503),
    JE_16W20A(501),
    JE_1_9_4(184),
    JE_1_9_3(183),
    JE_1_9_3_PRE3(182),
    JE_1_9_3_PRE2(181),
    JE_1_9_3_PRE1(180),
    JE_16W15B(179),
    JE_16W15A(178),
    JE_16W14A(177),
    JE_1_9_2(176),
    JE_1_9_1(175),
    JE_1_9_1_PRE3(172),
    JE_1_9_1_PRE2(171),
    JE_1_9_1_PRE1(170),
    JE_1_9(169),
    JE_1_9_PRE4(168),
    JE_1_9_PRE3(167),
    JE_1_9_PRE2(165),
    JE_1_9_PRE1(164),
    JE_16W07B(163),
    JE_16W07A(162),
    JE_16W06A(161),
    JE_16W05B(160),
    JE_16W05A(159),
    JE_16W04A(158),
    JE_16W03A(157),
    JE_16W02A(156),
    JE_15W51B(155),
    JE_15W51A(154),
    JE_15W50A(153),
    JE_15W49B(152),
    JE_15W49A(151),
    JE_15W47C(150),
    JE_15W47B(149),
    JE_15W47A(148),
    JE_15W46A(146),
    JE_15W45A(145),
    JE_15W44B(143),
    JE_15W44A(142),
    JE_15W43C(141),
    JE_15W43B(140),
    JE_15W43A(139),
    JE_15W42A(138),
    JE_15W41B(137),
    JE_15W41A(136),
    JE_15W40B(134),
    JE_15W40A(133),
    JE_15W39C(132),
    JE_15W39B(131),
    JE_15W39A(130),
    JE_15W38B(129),
    JE_15W38A(128),
    JE_15W37A(127),
    JE_15W36D(126),
    JE_15W36C(125),
    JE_15W36B(124),
    JE_15W36A(123),
    JE_15W35E(122),
    JE_15W35D(121),
    JE_15W35C(120),
    JE_15W35B(119),
    JE_15W35A(118),
    JE_15W34D(117),
    JE_15W34C(116),
    JE_15W34B(115),
    JE_15W34A(114),
    JE_15W33C(112),
    JE_15W33B(111),
    JE_15W33A(111),
    JE_15W32C(104),
    JE_15W32B(103),
    JE_15W32A(100),
    JE_1_6_4(137),
    JE_1_6_2(132),
    JE_1_6_1(129),
    JE_1_6_PRE(128),
    JE_13W26A(128),
    JE_13W25C(127),
    JE_13W25B(127),
    JE_13W25A(127),
    JE_13W24B(126),
    JE_13W24A(125),
    JE_13W23B(124),
    JE_13W23A(123),
    JE_13W22A(123),
    JE_13W21B(123),
    JE_13W21A(123),
    JE_13W19A(122),
    JE_13W18A(121),
    JE_13W17A(120),
    JE_13W16B(119),
    JE_1_5_2(117),
    JE_1_5_1(116),
    JE_1_5(116),
    JE_13W09B(115),
    JE_13W06A(114),
    JE_13W05B(112),
    JE_13W05A(111),
    JE_13W04A(111),
    JE_13W03A(104),
    JE_13W02A(103),
    JE_13W01A(100),
}