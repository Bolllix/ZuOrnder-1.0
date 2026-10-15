// Einstiegspunkt der Oberfläche: hängt die React-App in das <div id="root">
// aus index.html ein. StrictMode prüft während der Entwicklung auf typische Fehler.
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
