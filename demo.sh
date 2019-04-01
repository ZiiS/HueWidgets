#! /bin/bash

adb -e shell settings put global sysui_demo_allowed 1
adb -e shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1200
adb -e shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
adb -e shell am broadcast -a com.android.systemui.demo -e command network -e nosim hide
adb -e shell am broadcast -a com.android.systemui.demo -e command network -e mobile show -e level 3 -e datatype false -e fully true
adb -e shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 3
adb -e shell am broadcast -a com.android.systemui.demo -e command network -e fully true
adb -e shell am broadcast -a com.android.systemui.demo -e command battery -e plugged false -e level 60
read -p "press to disable"
adb -e shell am broadcast -a com.android.systemui.demo -e command exit

