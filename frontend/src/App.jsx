import { BrowserRouter, Route, Routes } from 'react-router-dom'
import ApplicationDetailPage from './pages/ApplicationDetailPage'
import CustomersPage from './pages/CustomersPage'
import LandingPage from './pages/LandingPage'
import NewApplicationPage from './pages/NewApplicationPage'
import PortalLayout from './pages/PortalLayout'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/portal" element={<PortalLayout />}>
          <Route path="customers" element={<CustomersPage />} />
          <Route path="applications/new" element={<NewApplicationPage />} />
          <Route path="applications/:id" element={<ApplicationDetailPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
