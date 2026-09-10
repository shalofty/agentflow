import { Link } from 'react-router-dom'

/**
 * Legacy /applications/new bookmarks no longer create drafts on mount.
 * Explicit create actions live on the customers page.
 */
export default function NewApplicationPage() {
  return (
    <div>
      <div className="alert alert-info" role="status">
        Applications are created from the customer list so drafts can be resumed
        after reload.
      </div>
      <Link to="/portal/customers" className="button button-primary">
        Go to customers
      </Link>
    </div>
  )
}
