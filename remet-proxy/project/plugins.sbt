// Copyright 2014 Brandon Arp
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// Comment to get more information during initialization
logLevel := Level.Info

// The Typesafe repository
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// The SBT Community repository
resolvers += "SBT Community repository" at "http://dl.bintray.com/sbt/sbt-plugin-releases/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.4.0-RC1")

addSbtPlugin("com.typesafe.sbt" % "sbt-aspectj" % "0.10.0")

addSbtPlugin("de.johoop" % "findbugs4sbt" % "1.4.0")

libraryDependencies ++= Seq(
    "com.puppycrawl.tools" % "checkstyle" % "6.3"
)
