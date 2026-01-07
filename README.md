# VexTeamApp

A professional, dark-themed Android application designed for VEX Robotics teams to track performance metrics in real-time using the RobotEvents API.

## Features

- **Team Verification**: Search and validate VEX teams (VRC/VEX U) using Regex and API confirmation.
- **Live Statistics**: Fetch real-time data for team organization, program, and location.
- **Skills Tracking**: View highest Driver and Programming skills scores for the current and past seasons.
- **Awards Gallery**: A dedicated view for all trophies and achievements earned by the team.
- **Tournament Insights**: Drill down into specific competitions to see rankings and match records.
- **Accessibility Ready**: Fully compliant with TalkBack and screen reader standards.

## Technical Stack

- **Language**: Kotlin.
- **UI Architecture**: View Binding with ConstraintLayout for responsive, high-performance layouts.
- **Networking**: Retrofit 2 for REST API communication with the RobotEvents v2 endpoint.
- **Concurrency**: Kotlin Coroutines for non-blocking UI updates.
- **Local Storage**: SharedPreferences for persistent Team ID storage.

## Setup & Installation

1. Clone this repository: `git clone https://github.com/Jose-Ayala/VexTeamAPP.git`
2. Open the project in **Android Studio**.
3. Ensure you have your RobotEvents API Key configured in `RetrofitClient.kt`.
4. Build and run the app on an emulator or physical device (Minimum SDK 24).

## Privacy Policy

We value your privacy. This app does not collect personal data. View our [Full Privacy Policy](https://github.com/Jose-Ayala/VexTeamAPP/blob/main/PRIVACY_POLICY.md).

## License

Distributed under the MIT License. See `LICENSE` for more information.
