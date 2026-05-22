package com.mobileaudiocast

import android.content.Context
import org.json.JSONObject
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpTransceiver
import org.webrtc.SessionDescription

class WebRtcStreamer(context: Context, private val signalingServer: SignalingServer) {
    private val peerConnectionFactory: PeerConnectionFactory
    private var peerConnection: PeerConnection? = null
    private var localAudioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null

    init {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
        )
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(null, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(null))
            .createPeerConnectionFactory()
    }

    fun initPeerConnection() {
        val rtcConfig = PeerConnection.RTCConfiguration(emptyList())
        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState) = Unit
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) = Unit
            override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) = Unit
            override fun onIceCandidate(candidate: org.webrtc.IceCandidate) {
                signalingServer.sendToClient(JSONObject().put("type", "ice-candidate").put("candidate", candidate.sdp).put("sdpMid", candidate.sdpMid).put("sdpMLineIndex", candidate.sdpMLineIndex))
            }
            override fun onIceCandidatesRemoved(candidates: Array<out org.webrtc.IceCandidate>) = Unit
            override fun onAddStream(stream: org.webrtc.MediaStream) = Unit
            override fun onRemoveStream(stream: org.webrtc.MediaStream) = Unit
            override fun onDataChannel(dc: org.webrtc.DataChannel) = Unit
            override fun onRenegotiationNeeded() = Unit
            override fun onAddTrack(receiver: org.webrtc.RtpReceiver, mediaStreams: Array<out org.webrtc.MediaStream>) = Unit
            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) = Unit
            override fun onTrack(transceiver: RtpTransceiver?) = Unit
        })

        localAudioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory.createAudioTrack("audio0", localAudioSource)
        peerConnection?.addTrack(localAudioTrack)
    }

    fun createOffer() {
        val constraints = MediaConstraints()
        peerConnection?.createOffer(object : org.webrtc.SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription) {
                peerConnection?.setLocalDescription(this, desc)
                signalingServer.sendToClient(JSONObject().put("type", "offer").put("sdp", desc.description))
            }
            override fun onSetSuccess() = Unit
            override fun onCreateFailure(error: String) = Unit
            override fun onSetFailure(error: String) = Unit
        }, constraints)
    }

    fun handleAnswer(sdp: String) {
        val answer = SessionDescription(SessionDescription.Type.ANSWER, sdp)
        peerConnection?.setRemoteDescription(object : org.webrtc.SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription) = Unit
            override fun onSetSuccess() = Unit
            override fun onCreateFailure(error: String) = Unit
            override fun onSetFailure(error: String) = Unit
        }, answer)
    }

    fun addIceCandidate(candidate: String, sdpMid: String, sdpMLineIndex: Int) {
        peerConnection?.addIceCandidate(org.webrtc.IceCandidate(sdpMid, sdpMLineIndex, candidate))
    }

    fun release() {
        peerConnection?.close()
        peerConnection = null
        localAudioTrack?.dispose(); localAudioTrack = null
        localAudioSource?.dispose(); localAudioSource = null
    }
}
