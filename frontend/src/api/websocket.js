import { getToken } from '../utils/auth'

export function createNotificationSocket(userId, handlers = {}, options = {}) {
  let socket = null
  let reconnectTimer = null
  let closedByUser = false
  const reconnectDelay = options.reconnectDelay || 3000

  function connect() {
    const token = getToken()
    if (!userId || !token) return null

    const baseURL = import.meta.env.VITE_WS_BASE_URL
    const url = `${baseURL}/ws/${userId}?token=${encodeURIComponent(token)}`
    console.log('[WS Connect]', url)
    socket = new WebSocket(url)

    socket.onopen = (event) => {
      console.log('[WS Open]', url)
      handlers.onOpen?.(event)
    }
    socket.onmessage = (event) => {
      let payload = event.data
      try {
        payload = JSON.parse(event.data)
      } catch {
        // The backend may send plain text for diagnostic messages.
      }
      console.log('[WS Message]', payload)
      handlers.onMessage?.(payload, event)
    }
    socket.onerror = (event) => {
      console.log('[WS Error]', event)
      handlers.onError?.(event)
    }
    socket.onclose = (event) => {
      console.log('[WS Close]', event.code, event.reason)
      handlers.onClose?.(event)
      socket = null
      if (!closedByUser && options.reconnect !== false) {
        reconnectTimer = window.setTimeout(connect, reconnectDelay)
      }
    }
    return socket
  }

  connect()

  return {
    get socket() {
      return socket
    },
    close() {
      closedByUser = true
      if (reconnectTimer) window.clearTimeout(reconnectTimer)
      socket?.close()
    }
  }
}
