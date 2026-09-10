import { BrowserRouter, Route, Routes } from 'react-router-dom'
import CustomersPage from './pages/CustomersPage'
import LandingPage from './pages/LandingPage'
import PortalLayout from './pages/PortalLayout'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/portal" element={<PortalLayout />}>
          <Route path="customers" element={<CustomersPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
