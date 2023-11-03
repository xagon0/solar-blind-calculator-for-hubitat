import java.time.format.DateTimeFormatter
import java.time.OffsetDateTime
import java.util.Date
import java.util.Calendar
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.ZoneId

metadata {
    definition(name: "SolarBlindCalculator", namespace: "NotBlindedByTheLight", author: "Ethan Frances") {
        attribute "elevationResult", "DOUBLE"
        attribute "azimuthResult", "DOUBLE"
        attribute "blindPercentage", "DOUBLE"
 
        command "getBlindPercentage", [
            [name: "windowAzimuthMin*", type: "NUMBER", description: "Window Start Azimuth"],
             [name: "windowAzimuthMax*", type: "NUMBER", description: "Window End Azimuth"],
             [name: "windowMinElevation*", type: "NUMBER", description: "Window Minimum Elevation"],
             [name: "windowMaxElevation*", type: "NUMBER", description: "Window Maximum Elevation"],
             [name: "blindMinPercentage*", type: "NUMBER", description: "Blind Percentage at Minimum Elevation"],
             [name: "blindMaxPercentage*", type: "NUMBER", description: "Blind Percentage at Maximum Elevation"],
             [name: "blindMaxPercentageOutsideField*", type: "NUMBER", description: "Blind Percentage when Sun Outside"]

        ]
        
        command "getSolarAzimuthElevation"
        
    }
    preferences {
        section {
            input(type: "DOUBLE", name: "Latitude",  title: "<font color='0000AA'><b>Latitude</b></font>", required: true)
            input(type: "DOUBLE", name: "Longitude", title: "<font color='0000AA'><b>Longitude</b></font>", required: true) 
        }
    }
}


void uninstalled() {
    log.trace("Uninstalled")
}

def updated() {
    log.trace("Updated")
}

def getBlindPercentage(windowAzimuthMin, windowAzimuthMax, windowMinElevation, windowMaxElevation, blindMinPercentage, blindMaxPercentage, blindMaxPercentageOutsideField){

    // Get the current UTC date and time
    ZonedDateTime utcDateTime = ZonedDateTime.now(ZoneId.of('UTC'));
    
    // Parse the latitude and longitude from string to double.
    // It's assumed that 'Latitude' and 'Longitude' are defined elsewhere and valid.
    double latitude = Double.parseDouble(Latitude);
    double longitude = Double.parseDouble(Longitude);

    // Calculate the solar angle based on the latitude, longitude, and current UTC time
    def result = calculateSolarAngle(latitude, longitude, utcDateTime);
    def elevation = result[0]; // Extract the solar elevation from the result
    def azimuth = result[1]; // Extract the solar azimuth from the result

    // Calculate the percentage of sunlight that the blinds should block
    def percentage = calculatePercentage(
        azimuth.toBigDecimal(), elevation.toBigDecimal(), 
        windowAzimuthMin.toBigDecimal(), windowAzimuthMax.toBigDecimal(), 
        windowMaxElevation.toBigDecimal(), windowMinElevation.toBigDecimal(), 
        blindMaxPercentage.toInteger(), blindMinPercentage.toInteger(), 
        blindMaxPercentageOutsideField.toInteger()
    );

    // Send events to update the blind percentage and solar angle values
    sendEvent(name: "blindPercentage", value: percentage);
    sendEvent(name: "elevationResult", value: elevation);
    sendEvent(name: "azimuthResult", value: azimuth);
}

def calculatePercentage(double sunAzimuth, double sunElevation, 
                        double windowAzimuthMin, double windowAzimuthMax, double windowMaxElevation, 
                        double windowMinElevation, int windowMaxPercentage, 
                        int windowMinPercentage, int blindMaxPercentageOutsideField) {
    
    // Check if the sun is within the azimuth range of the window.
    if (!(sunAzimuth > windowAzimuthMin && sunAzimuth < windowAzimuthMax)) {
        return blindMaxPercentageOutsideField // Sun is not affecting the window
    }


    // Check if the elevation is below the minimum threshold.
    if (sunElevation < windowMinElevation) {
        return windowMinPercentage // Sun is too low, set to minimum percentage
    }
    
    // Check if the elevation is above the maximum threshold.
    if (sunElevation > windowMaxElevation) {
        return windowMaxPercentage // Sun is too high, set to maximum percentage
    }

    // Calculate the percentage based on the elevation range and corresponding blind position range.
    double elevationRange = windowMaxElevation - windowMinElevation
    double percentageRange = windowMaxPercentage - windowMinPercentage
    double elevationDifference = sunElevation - windowMinElevation
    double percentage = windowMinPercentage + (percentageRange * (elevationDifference / elevationRange))

    return percentage.round() // Return the rounded percentage value
}


def getSolarAzimuthElevation() {
    // Get the current UTC time as ZonedDateTime.
    ZonedDateTime utcDateTime = ZonedDateTime.now(ZoneId.of('UTC'))

    // Parse latitude and longitude values from device settings or input.
    double latitude = Double.parseDouble(Latitude) // Ensure 'Latitude' is defined and valid.
    double longitude = Double.parseDouble(Longitude) // Ensure 'Longitude' is defined and valid.

    // Calculate solar angles based on geographic coordinates and current UTC time.
    def result = calculateSolarAngle(latitude, longitude, utcDateTime)
    def elevation = result[0]
    def azimuth = result[1]

    // Create events to update the attributes for solar angles.
    sendEvent(name: "elevationResult", value: elevation)
    sendEvent(name: "azimuthResult", value: azimuth)
    sendEvent(name: "blindPercentage", value: null)
}

// Calculates solar elevation and azimuth angles.
def calculateSolarAngle(double latitude, double longitude, ZonedDateTime utcDateTime) {
    // Constants for conversion between degrees and radians.
    final double DEG_TO_RAD = Math.PI / 180.0
    final double RAD_TO_DEG = 180.0 / Math.PI

    // Calculate solar position based on date and time.
    def result = solarPositionCalculations(utcDateTime)
    def declination = result[0]
    def equationOfTime = result[1]

    // Calculate the true solar time in minutes.
    def trueSolarTime = calculateTrueSolarTime(utcDateTime, longitude, equationOfTime)

    // Calculate the hour angle in radians.
    def hourAngle = calculateHourAngle(trueSolarTime)

    // Calculate the solar zenith and elevation angles.
    def solarZenithAngle = calculateSolarZenithAngle(latitude * DEG_TO_RAD, declination, hourAngle)
    def solarElevationAngle = (90 - solarZenithAngle * RAD_TO_DEG) + atmosphericRefractionCorrection(solarZenithAngle * RAD_TO_DEG)

    // Calculate the solar azimuth angle in degrees.
    def solarAzimuthAngle = calculateSolarAzimuthAngle(hourAngle, latitude * DEG_TO_RAD, declination) * RAD_TO_DEG

    // Adjust azimuth angle to fit within the 0-360 degree range.
    solarAzimuthAngle = (solarAzimuthAngle + 180) % 360

    return [solarElevationAngle, solarAzimuthAngle]
}

// Performs the solar position calculations based on the current date and time.
def solarPositionCalculations(ZonedDateTime utcDateTime) {
    // Calculate day of the year.
    def dayOfYear = utcDateTime.getDayOfYear()
    
    // Calculate the current fraction of the year in radians.
    def gamma = 2.0 * Math.PI / 365.0 * (dayOfYear - 1 + (utcDateTime.getHour() - 12.0) / 24.0)

    // Equations for solar declination and equation of time.
    def declination = calculateSolarDeclination(gamma)
    def equationOfTime = calculateEquationOfTime(gamma)

    return [declination, equationOfTime]
}

// Equation of Time (EoT) and Declination calculations.
def calculateSolarDeclination(double gamma) {
    // Simplified formula to calculate the solar declination.
    // Declination is the angle between the rays of the sun and the plane of the earth's equator.
    return (0.006918 - 0.399912 * Math.cos(gamma) + 0.070257 * Math.sin(gamma)
            - 0.006758 * Math.cos(2 * gamma) + 0.000907 * Math.sin(2 * gamma)
            - 0.002697 * Math.cos(3 * gamma) + 0.00148 * Math.sin(3 * gamma))
}

def calculateEquationOfTime(double gamma) {
    // Simplified formula to calculate the equation of time.
    // EoT is the discrepancy between two kinds of solar time.
    return 229.18 * (0.000075 + 0.001868 * Math.cos(gamma) - 0.032077 * Math.sin(gamma)
                    - 0.014615 * Math.cos(2 * gamma) - 0.040849 * Math.sin(2 * gamma))
}

// Calculate the True Solar Time (TST).
def calculateTrueSolarTime(ZonedDateTime utcDateTime, double longitude, double equationOfTime) {
    // Convert UTC Date to total minutes since midnight.
    def totalMinutes = utcDateTime.getHour() * 60 + utcDateTime.getMinute() + utcDateTime.getSecond() / 60.0

    // Calculate the true solar time.
    return (totalMinutes + equationOfTime + 4 * (longitude - utcDateTime.getOffset().getTotalSeconds() / 3600) - 60 * 0) % 1440
}

// Calculate the Hour Angle (HA).
def calculateHourAngle(double trueSolarTime) {
    // The hour angle is the measure of time since solar noon in degrees.
    def hourAngle = (trueSolarTime / 4.0) - 180.0
    hourAngle += (hourAngle < -180) ? 360 : 0
    return hourAngle * (Math.PI / 180.0) // Convert to radians.
}

// Calculate the Solar Zenith Angle (SZA).
def calculateSolarZenithAngle(double latitude, double declination, double hourAngle) {
    // Zenith angle is the angle between the vertical direction and the line to the sun.
    return Math.acos(Math.sin(latitude) * Math.sin(declination) + Math.cos(latitude) * Math.cos(declination) * Math.cos(hourAngle))
}

// Calculate the atmospheric refraction correction based on the solar zenith angle.
def atmosphericRefractionCorrection(double solarZenithAngle) {
    // Calculate solar elevation angle
    def solarElevationAngle = 90 - solarZenithAngle * (Math.PI / 180.0)

    // Calculate atmospheric refraction correction
    def atmosphericRefractionCorrection = (solarElevationAngle > 85) ? 0 :
        (solarElevationAngle > 5) ? 58.1 / Math.tan(solarElevationAngle * deg2rad) - 
                                    0.07 / Math.pow(Math.tan(solarElevationAngle * deg2rad), 3) + 
                                    0.000086 / Math.pow(Math.tan(solarElevationAngle * deg2rad), 5) :
        (solarElevationAngle > -0.575) ? 1735 + solarElevationAngle * 
                                         (-518.2 + solarElevationAngle * (103.4 + solarElevationAngle * 
                                         (-12.79 + solarElevationAngle * 0.711))) : -20.772 / Math.tan(solarElevationAngle * deg2rad)

    atmosphericRefractionCorrection /= 3600
    return atmosphericRefractionCorrection
}

// Calculate the Solar Azimuth Angle (SAA).
def calculateSolarAzimuthAngle(double hourAngle, double latitude, double declination) {
    // Azimuth angle is the compass direction from which the sunlight is coming.
    return Math.atan2(Math.sin(hourAngle), Math.cos(hourAngle) * Math.sin(latitude) - Math.tan(declination) * Math.cos(latitude))
}
