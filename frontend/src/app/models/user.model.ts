export interface User {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  role: 'CLIENT' | 'SELLER';
  avatarUrl?: string;
  // Add any other fields you need
}