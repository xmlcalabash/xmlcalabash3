package com.xmlcalabash.app

import java.time.Instant

/** @suppress
 * Adapted from: https://celestialprogramming.com/meeus-illuminated_fraction_of_the_moon.html
 * Greg Miller gmiller@gregmiller.net 2021
 * http://www.celestialprogramming.com/
 * Released as public domain
 *
 * This class is suppressed in the documentation because it's really just an Easter egg.
*/
object Moon {
    fun illumination(time: Instant = Instant.now()): Double {
        val toRad = Math.PI / 180.0;
        val jd = julianDateFromUnixTime(time)
        val T = (jd-2451545.0)/36525.0;

        val D = constrain(297.8501921 + 445267.1114034*T - 0.0018819*T*T + 1.0/545868.0*T*T*T - 1.0/113065000.0*T*T*T*T)*toRad; //47.2
        val M = constrain(357.5291092 + 35999.0502909*T - 0.0001536*T*T + 1.0/24490000.0*T*T*T)*toRad; //47.3
        val Mp = constrain(134.9633964 + 477198.8675055*T + 0.0087414*T*T + 1.0/69699.0*T*T*T - 1.0/14712000.0*T*T*T*T)*toRad; //47.4

        //48.4
        val i= constrain(180 - D*180/Math.PI - 6.289 * Math.sin(Mp) + 2.1 * Math.sin(M) -1.274 * Math.sin(2*D - Mp) -0.658 * Math.sin(2*D) -0.214 * Math.sin(2*Mp) -0.11 * Math.sin(D))*toRad;

        return (1+Math.cos(i))/2;
    }

    private fun julianDateFromUnixTime(time: Instant): Double {
        //Not valid for dates before Oct 15, 1582
        return (time.toEpochMilli() / 86400000.0) + 2440587.5;
    }

    private fun unixTimeFromJulianDate(jd: Double): Double{
        //Not valid for dates before Oct 15, 1582
        return (jd-2440587.5)*86400000.0;
    }

    private fun constrain(d: Double): Double {
        val t= d % 360;
        if (t < 0) {
            return t + 360.0
        }
        return t
    }
}
