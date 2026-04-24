import cv2
import numpy as np
import os
import sys

def create_seamless_video(input_path, output_path, fade_duration_sec=0.5):
    """
    Creates a version of the video where the last frame matches the first frame.
    It does this by blending the first frame over the last few frames of the video.
    """
    if not os.path.exists(input_path):
        print(f"Error: File {input_path} not found.")
        return

    cap = cv2.VideoCapture(input_path)
    if not cap.isOpened():
        print("Error: Could not open video.")
        return

    # Get video properties
    fps = cap.get(cv2.CAP_PROP_FPS)
    width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
    total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    
    # Read the first frame
    ret, first_frame = cap.read()
    if not ret:
        print("Error: Could not read first frame.")
        return

    # Calculate fade frames
    fade_frames = int(fps * fade_duration_sec)
    
    # Reset capture to start
    cap.set(cv2.CAP_PROP_POS_FRAMES, 0)

    # Define codec and output
    fourcc = cv2.VideoWriter_fourcc(*'mp4v')
    out = cv2.VideoWriter(output_path, fourcc, fps, (width, height))

    print(f"Processing video: {width}x{height}, {fps} FPS, {total_frames} frames")
    print(f"Applying {fade_duration_sec}s fade to match start/end frames...")

    frame_idx = 0
    while True:
        ret, frame = cap.read()
        if not ret:
            break

        # Check if we are in the fade-out zone (the very end)
        if frame_idx > total_frames - fade_frames:
            # Calculate alpha (0.0 to 1.0)
            alpha = (frame_idx - (total_frames - fade_frames)) / fade_frames
            # Blend current frame with the FIRST frame
            blended_frame = cv2.addWeighted(frame, 1 - alpha, first_frame, alpha, 0)
            out.write(blended_frame)
        else:
            out.write(frame)

        frame_idx += 1
        if frame_idx % 30 == 0:
            print(f"Progress: {int(frame_idx/total_frames*100)}%")

    cap.release()
    out.release()
    print(f"\nSuccess! Saved to {output_path}")
    print("This video now has an end frame identical to its start frame.")

if __name__ == "__main__":
    print("--- Video Re-engineering Tool ---")
    print("Usage: python reengineer_video.py <input_video_path>")
    
    if len(sys.argv) < 2:
        input_file = input("Enter the path to your video file: ").strip('"')
    else:
        input_file = sys.argv[1]

    output_file = "seamless_" + os.path.basename(input_file)
    create_seamless_video(input_file, output_file)
