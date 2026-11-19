import { Navigate, Route, Routes } from 'react-router-dom';
import PublicPage from './public/PublicPage';
import AdminGate from './admin/AdminGate';
import WorkspaceGate from './workspace/WorkspaceGate';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<PublicPage />} />
      <Route path="/app" element={<WorkspaceGate />} />
      <Route path="/admin" element={<AdminGate />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
