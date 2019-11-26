import os
import json

path = os.path.dirname(os.path.abspath(__file__))

with open("{}/conf.json".format(path)) as json_read_file:
    data = json.load(json_read_file)
json_read_file.close()

data["initial_setting"] = "false"
data["id"] = ""

with open("{}/conf.json".format(path), 'w') as json_write_file:
    json.dump(data, json_write_file)
json_write_file.close()

f = open("/etc/network/interfaces", "w")
f.write("# interfaces(5) file used by ifup(8) and ifdown(8)\n"
        "# Include files from /etc/network/interfaces.d:\n"
        "source-directory /etc/network/interfaces.d\n\n"
        "auto wlan0\n"
        "iface wlan0 inet static\n"
        "address 10.10.0.1\n"
        "netmask 255.255.255.0")
f.close()

os.system("sudo reboot")