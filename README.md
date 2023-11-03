# SolarBlindCalculator

Welcome to the SolarBlindCalculator! This driver is aimed at helping individuals and smart-home enthusiasts calculate the optimal blind adjustments to control the amount of sunlight entering a room with Hubitat.

## Description

The SolarBlindCalculator is a smart solution crafted to enhance the comfort of your living or workspace. It intelligently computes the percentage of sunlight that can be blocked by window blinds based on the current solar azimuth and elevation, in relation to your window's orientation.

## Features

- Calculate blind coverage percentage based on solar position
- Customize calculations based on window azimuth and elevation range
- Utilize geographical coordinates for precise solar positioning

## Getting Started

### Prerequisites

- Latitude / Longitude of your Location

### Installation

1. Navigate to the **Drivers Code** section of your Hubitat interface.
2. Click on the **+ New Driver** button.
3. Copy the content of `solarBlindCalculator_Driver.groovy` and paste it into Hubitat.
4. Save the driver.
5. Create a new virtual device and assign the "Solar Blind Calculator Driver" as its driver.

### Configuration

Once the driver is added to a device, you will have the option to configure:

- **Latitude**: Your current locations latitude.
- **Longitude**: Your current locations longitude.
- 
## Usage

Two primary commands can be executed:

1. **getBlindPercentage(windowAzimuthMin, windowAzimuthMax, windowMinElevation, windowMaxElevation, blindMinPercentage, blindMaxPercentage, blindMaxPercentageOutsideField)**: Updates the azimuth, elevation, and calculated blind percentage.
2. **getSolarAzimuthElevation()**: Updates the azimuth, and elevation for your own calculation.

## Contributing

Feel free to fork, modify, and submit pull requests. All contributions are welcome!

## License

This project is licensed under the MIT License - see the LICENSE.md file for details.
