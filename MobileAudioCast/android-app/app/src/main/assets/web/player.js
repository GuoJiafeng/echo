const statusEl = document.getElementById('status');
const audioEl = document.getElementById('audio');
let ws; let pc;

function setStatus(s){ statusEl.textContent = `状态：${s}`; }

function send(msg){ ws && ws.readyState === 1 && ws.send(JSON.stringify(msg)); }

function connect(){
  setStatus('信令连接中');
  ws = new WebSocket(`ws://${location.hostname}:8081/signaling`);
  ws.onopen = () => setStatus('WebRTC 连接中');
  ws.onclose = () => setStatus('已断开');
  ws.onmessage = async (e) => {
    const msg = JSON.parse(e.data);
    if (msg.type === 'offer') {
      await createPeer();
      await pc.setRemoteDescription({type: 'offer', sdp: msg.sdp});
      const answer = await pc.createAnswer();
      await pc.setLocalDescription(answer);
      send({type: 'answer', sdp: answer.sdp});
    } else if (msg.type === 'ice-candidate' && pc) {
      await pc.addIceCandidate({candidate: msg.candidate, sdpMid: msg.sdpMid, sdpMLineIndex: msg.sdpMLineIndex});
    }
  };
}

async function createPeer(){
  pc = new RTCPeerConnection({iceServers: []});
  pc.onicecandidate = (e) => { if (e.candidate) send({type:'ice-candidate', candidate:e.candidate.candidate, sdpMid:e.candidate.sdpMid, sdpMLineIndex:e.candidate.sdpMLineIndex}); };
  pc.ontrack = (e) => { audioEl.srcObject = e.streams[0]; setStatus('正在播放'); };
  pc.onconnectionstatechange = () => {
    if (['failed','disconnected','closed'].includes(pc.connectionState)) setStatus('已断开');
  };
}

document.getElementById('connectBtn').onclick = connect;
document.getElementById('reconnectBtn').onclick = () => { ws?.close(); pc?.close(); connect(); };
