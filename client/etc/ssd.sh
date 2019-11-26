sudo nvpmodel -m 0
sudo jetson_clocks
python3 /home/catch/ws/jetson-inference/build/aarch64/bin/detectnet-camera.py --camera /dev/video1