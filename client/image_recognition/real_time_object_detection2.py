# USAGE
# python real_time_object_detection2.py --prototxt MobileNetSSD_deploy.prototxt.txt --model MobileNetSSD_deploy.caffemodel

# import the necessary packages
import socket

from imutils.video import VideoStream
from imutils.video import FPS
import numpy as np
import argparse
import imutils
import time
import cv2
import time

# construct the argument parse and parse the arguments
ap = argparse.ArgumentParser()
ap.add_argument("-p", "--prototxt", required=True,
                help="path to Caffe 'deploy' prototxt file")
ap.add_argument("-m", "--model", required=True,
                help="path to Caffe pre-trained model")
ap.add_argument("-c", "--confidence", type=float, default=0.2,
                help="minimum probability to filter weak detections")
args = vars(ap.parse_args())

# initialize the list of class labels MobileNet SSD was trained to
# detect, then generate a set of bounding box colors for each class
CLASSES = ["background", "aeroplane", "bicycle", "bird", "boat",
           "bottle", "bus", "car", "cat", "chair", "cow", "diningtable",
           "dog", "horse", "motorbike", "person", "pottedplant", "sheep",
           "sofa", "train", "tvmonitor"]
COLORS = np.random.uniform(0, 255, size=(len(CLASSES), 3))

# coord is 왼쪽 위, 왼쪽 아래, 오른쪽 위, 오른쪽 아래.
# coord is not clockwise ~
Cx1, Cy1, Cx2, Cy2, Cx3, Cy3, Cx4, Cy4 = 0, 0, 0, 0, 0, 0, 0, 0
PWid, PHei = 1280, 720


def coord(boxx):
    global cnt, Cx1, Cy1, Cx2, Cy2, Cx3, Cy3, Cx4, Cy4

    Cx1 = boxx[0][0]
    Cy1 = boxx[0][1]

    Cx2 = boxx[1][0]
    Cy2 = boxx[1][1]


    Cx3 = boxx[2][0]
    Cy3 = boxx[2][1]

    Cx4 = boxx[3][0]
    Cy4 = boxx[3][1]

    # 1 is smallest
    if (Cx1 + Cy1) > (Cx2 + Cy2):
        Cx1, Cx2 = Cx2, Cx1
        Cy1, Cy2 = Cy2, Cy1

    if (Cx1 + Cy1) > (Cx3 + Cy3):
        Cx1, Cx3 = Cx3, Cx1
        Cy1, Cy3 = Cy3, Cy1

    if (Cx1 + Cy1) > (Cx4 + Cy4):
        Cx1, Cx4 = Cx4, Cx1
        Cy1, Cy4 = Cy4, Cy1

    # 4 is biggest

    if (Cx4 + Cy4) < (Cx2 + Cy2):
        Cx2, Cx4 = Cx4, Cx2
        Cy2, Cy4 = Cy4, Cy2

    if (Cx4 + Cy4) < (Cx3 + Cy3):
        Cx4, Cx3 = Cx3, Cx4
        Cy4, Cy3 = Cy3, Cy4

    # 2.y > 3.y and 2.x < 3.x, but I'll check just y coord.

    if Cy2 < Cy3:
        Cx2, Cx3 = Cx3, Cx2
        Cy2, Cy3 = Cy3, Cy2


def CameraProjectorCalibration(FirstImg):
    global cnt, Cx1, Cy1, Cx2, Cy2, Cx3, Cy3, Cx4, Cy4, PWid, PHei

    InputPoint = np.float32([[Cx1, Cy1], [Cx2, Cy2], [Cx3, Cy3], [Cx4, Cy4]])
    OutputPoint = np.float32([[0, 0], [0, PHei], [PWid, 0], [PWid, PHei]])

    # print([[Cx1, Cy1], [Cx2, Cy2], [Cx3, Cy3], [Cx4, Cy4]])

    # pts1의 좌표에 표시. perspective 변환 후 이동 점 확인.
    cv2.circle(FirstImg, (Cx1, Cy1), 5, (255, 0, 0), -1)
    cv2.circle(FirstImg, (Cx2, Cy2), 5, (0, 255, 0), -1)
    cv2.circle(FirstImg, (Cx3, Cy3), 5, (0, 0, 255), -1)
    cv2.circle(FirstImg, (Cx4, Cy4), 5, (0, 0, 0), -1)

    Mat = cv2.getPerspectiveTransform(InputPoint, OutputPoint)
    retImg = cv2.warpPerspective(FirstImg, Mat, (PWid, PHei))
    # retImg = cv2.warpAffine(FirstImg, Mat, (PWid, PHei))
    cv2.imshow('image2', FirstImg)
    return retImg


# load our serialized model from disk
print("[INFO] loading model...")
net = cv2.dnn.readNetFromCaffe(args["prototxt"], args["model"])

# initialize the video stream, allow the cammera sensor to warmup,
# and initialize the FPS counter
print("[INFO] starting video stream...")
vs = VideoStream(src=0).start()
time.sleep(2.0)
fps = FPS().start()

# Starting a Setting


while True:
    frame = vs.read()
    cv2.imshow("ProjectorIniting", frame)

    # gray ~
    gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)

    # clahe는 이미지를 블록 별로 히스토그램 균일화 해줌
    clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
    gray = clahe.apply(gray)

    # Laplacian trans
    laplace = cv2.Laplacian(gray, ddepth=cv2.CV_8U, ksize=3, scale=1, delta=0)
    cv2.imshow("Laplace", laplace)

    # bilateral blur, which keeps edges
    blurred = cv2.bilateralFilter(laplace, 13, 50, 50)

    # use simple thresholding. adaptive thresholding might be more robust
    (_, thresh) = cv2.threshold(blurred, 55, 255, cv2.THRESH_BINARY)
    cv2.imshow("Thresholded", thresh)

    # do some morphology to isolate just the barcode blob
    kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (9, 9))
    thresh = cv2.morphologyEx(thresh, cv2.MORPH_CLOSE, kernel)
    thresh = cv2.erode(thresh, None, iterations=4)
    thresh = cv2.dilate(thresh, None, iterations=4)
    cv2.imshow("After morphology", thresh)

    #retval = cv2.imwrite("blob", thresh)

    # find contours , 가장 바깥쪽의 라인, 그리고 컨투어 라인을 그릴 수 있는 포인트만.
    (cnts, _) = cv2.findContours(thresh, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

    if cnts:
        c = sorted(cnts, key=cv2.contourArea, reverse=True)[0]
        rect = cv2.minAreaRect(c)
        box = np.int0(cv2.boxPoints(rect))
        cv2.drawContours(frame, [box], -1, (0, 255, 0), 3)
        print(box)
        coord(box)
        cv2.imshow("ProjectorIniting", frame)

        cv2.waitKey(0)
        break

# Videoing

while True:

    frame = vs.read()
    frame = CameraProjectorCalibration(frame)
    # frame = imutils.resize(frame, width=400)

    # grab the frame dimensions and convert it to a blob
    (h, w) = frame.shape[:2]
    blob = cv2.dnn.blobFromImage(cv2.resize(frame, (300, 300)), 0.007843, (300, 300), 127.5)

    # pass the blob through the network and obtain the detections and
    # predictions
    net.setInput(blob)
    detections = net.forward()

    # loop over the detections
    print("======================================")

    for i in np.arange(0, detections.shape[2]):
        # extract the confidence (i.e., probability) associated with
        # the prediction
        confidence = detections[0, 0, i, 2]

        # filter out weak detections by ensuring the `confidence` is
        # greater than the minimum confidence
        if confidence > args["confidence"]:
            # extract the index of the class label from the
            # `detections`, then compute the (x, y)-coordinates of
            # the bounding box for the object
            idx = int(detections[0, 0, i, 1])
            box = detections[0, 0, i, 3:7] * np.array([w, h, w, h])
            (startX, startY, endX, endY) = box.astype("int")

            # draw the prediction on the frame
            label = "{}: {:.2f}%".format(CLASSES[idx], confidence * 100)

            if int(confidence*100) < 80:
                continue

            arr = ["cat", "dog", "person", "sheep", "horse", "cow"]
            for stri in arr:
                if CLASSES[idx] == stri:
                    # send_data = "send_cat_info" + "send_cat_info" + " " + str(int(startX)) + " " + str(int(startY)) + " " + str(int(endX)) + " " + str(int(endY))
                    # socket.send(send_data.encode())
                    print(CLASSES[idx], startX, startY, endX, endY)
                    break

            cv2.rectangle(frame, (startX, startY), (endX, endY), COLORS[idx], 2)
            y = startY - 15 if startY - 15 > 15 else startY + 15
            cv2.putText(frame, label, (startX, y),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.5, COLORS[idx], 2)

    # Output
    cv2.imshow("Frame", frame)
    key = cv2.waitKey(1) & 0xFF

    # Q is finish
    if key == ord("q"):
        break

    # update the FPS counter
    fps.update()

# Stop the timer and display FPS information
fps.stop()
print("[INFO] elapsed time: {:.2f}".format(fps.elapsed()))
print("[INFO] approx. FPS: {:.2f}".format(fps.fps()))

# Finish
cv2.destroyAllWindows()
vs.stop()
