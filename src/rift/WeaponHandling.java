package rift;

/** RIFT tuning in radians, seconds and metres. See THIRD_PARTY_NOTICES.md for references. */
enum WeaponHandling {
    SPARK(.006,.006,.065,.003,.020,3,.72,.0025,.020,.26,32,.018,.10,.76),
    VEIL(.0048,.004,.050,.0025,.018,4,.66,.0018,.018,.23,34,.016,.10,.78),
    TALON(.019,.012,.130,.004,.028,1,.70,.0035,.032,.48,24,.024,.12,.88),
    RUSH(.007,.006,.085,.004,.030,3,.86,.0036,.034,.24,30,.020,.11,.65),
    DART(.022,.010,.100,.004,.024,1,.68,.004,.030,.42,30,.013,.10,.16),
    WISP(.0055,.006,.066,.003,.024,4,.74,.0026,.026,.20,32,.014,.10,.70),
    CIRCUIT(.007,.0065,.082,.004,.030,3,.82,.003,.030,.22,30,.016,.11,.66),
    ECHO(.006,.0085,.080,.004,.034,4,.72,.0024,.028,.28,27,.018,.10,.86),
    SHADE(.0045,.006,.060,.003,.026,5,.66,.002,.024,.24,30,.018,.10,.88),
    HELIX(.004,.005,.048,.0025,.020,4,.70,.0018,.022,.20,32,.019,.11,.85),
    MARROW(.020,.012,.100,.004,.025,1,.65,.004,.028,.28,27,.012,.10,.20),
    BREACH(.015,.010,.095,.004,.028,2,.78,.0045,.032,.39,25,.014,.11,.18),
    RIDGE(.017,.012,.110,.003,.024,2,.62,.0025,.024,.38,26,.026,.14,.93),
    HORIZON(.026,.016,.140,.004,.030,1,.60,.004,.030,.40,24,.035,.18,.96),
    BASTION(.0075,.008,.095,.005,.040,3,.58,.003,.036,.34,25,.024,.12,.84);

    final double rise,riseGrowth,riseCap,sideGrowth,sideCap,sideFrequency,bloomGrowth,bloomCap;
    final int sideStart;
    final double recoveryDelay,recoveryRate,moveSpread,airSpread,rangeRetention;

    WeaponHandling(double rise,double riseGrowth,double riseCap,double sideGrowth,double sideCap,
                   int sideStart,double sideFrequency,double bloomGrowth,double bloomCap,
                   double recoveryDelay,double recoveryRate,double moveSpread,double airSpread,double rangeRetention){
        this.rise=rise;this.riseGrowth=riseGrowth;this.riseCap=riseCap;
        this.sideGrowth=sideGrowth;this.sideCap=sideCap;this.sideStart=sideStart;this.sideFrequency=sideFrequency;
        this.bloomGrowth=bloomGrowth;this.bloomCap=bloomCap;this.recoveryDelay=recoveryDelay;this.recoveryRate=recoveryRate;
        this.moveSpread=moveSpread;this.airSpread=airSpread;this.rangeRetention=rangeRetention;
    }
}
