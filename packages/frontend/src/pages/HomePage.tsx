import { Container, Row, Col } from "react-bootstrap";
import ServiceCard from "../components/ServiceCard";
import type { Service } from "../components/ServiceCard";

const services: Service[] = [
  {
    id: 1,
    title: "Calculus & Statistics Tutoring",
    category: "tutoring",
    provider: { name: "Sarah Chen", avatar: "👩‍🎓", rating: 4.9, reviews: 47 },
    price: "$25/hr",
    description: "Math major offering help with Calc I-III, Linear Algebra, and Statistics. 3 years of tutoring experience with proven results.",
    location: "Main Library or Online",
    tags: ["Mathematics", "One-on-one", "Group sessions"],
  },
  {
    id: 2,
    title: "Computer Repair & Setup",
    category: "tech",
    provider: { name: "Marcus Johnson", avatar: "👨‍💻", rating: 4.8, reviews: 63 },
    price: "$30-50",
    description: "Fast computer repairs, software installation, virus removal, and hardware upgrades. Same-day service available.",
    location: "On-campus or dorm visits",
    tags: ["Hardware", "Software", "Emergency service"],
  },
  {
    id: 3,
    title: "Photography for Events",
    category: "photography",
    provider: { name: "Emily Rodriguez", avatar: "📸", rating: 5.0, reviews: 31 },
    price: "$100-200",
    description: "Professional photography for formals, club events, headshots, and grad photos. High-quality edits included.",
    location: "Anywhere on campus",
    tags: ["Events", "Portraits", "Editing included"],
  },
  {
    id: 4,
    title: "Resume Review & Career Coaching",
    category: "finance",
    provider: { name: "David Park", avatar: "👔", rating: 4.9, reviews: 28 },
    price: "$20/session",
    description: "Former McKinsey intern helping with resumes, cover letters, interview prep, and career strategy.",
    location: "Student Center or Zoom",
    tags: ["Resume", "Interview prep", "Career advice"],
  },
  {
    id: 5,
    title: "Home-Cooked Meal Prep",
    category: "food",
    provider: { name: "Priya Patel", avatar: "👩‍🍳", rating: 4.7, reviews: 52 },
    price: "$15-25/meal",
    description: "Healthy, homemade meals delivered to your dorm. Vegetarian, vegan, and dietary restrictions accommodated.",
    location: "Campus delivery",
    tags: ["Healthy", "Custom orders", "Meal plans"],
  },
  {
    id: 6,
    title: "Web Development & Design",
    category: "tech",
    provider: { name: "Alex Kim", avatar: "💻", rating: 4.9, reviews: 19 },
    price: "$40/hr",
    description: "Build websites for clubs, personal portfolios, or small businesses. React, WordPress, and custom solutions.",
    location: "Remote collaboration",
    tags: ["Web design", "React", "Portfolio"],
  },
  {
    id: 7,
    title: "Sublet: 1BR Apartment Summer",
    category: "housing",
    provider: { name: "Jessica Martinez", avatar: "🏠", rating: 4.6, reviews: 8 },
    price: "$800/month",
    description: "Furnished 1-bedroom apartment 5 min walk from campus. Available June-August. Utilities included.",
    location: "123 College Ave",
    tags: ["Furnished", "Utilities included", "Pet-friendly"],
  },
  {
    id: 8,
    title: "Python & Data Science Tutoring",
    category: "tutoring",
    provider: { name: "Ryan Thompson", avatar: "🐍", rating: 4.8, reviews: 34 },
    price: "$30/hr",
    description: "CS grad student specializing in Python, machine learning, data analysis, and pandas/numpy libraries.",
    location: "Engineering Building or Online",
    tags: ["Python", "ML", "Data Science"],
  },
];

function HomePage() {
  return (
    <div style={{ backgroundColor: "#f4f4f4", minHeight: "100vh", padding: "40px 0" }}>
      <Container>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "24px" }}>
          <h1 style={{ fontWeight: "700", fontSize: "1.8rem", margin: 0 }}>Campus Services</h1>
        </div>
        <p style={{ color: "#666", marginBottom: "24px" }}>{services.length} services found</p>
        <Row>
          {services.map((service) => (
            <Col key={service.id} xs={12} md={6} lg={4} style={{ marginBottom: "24px" }}>
              <ServiceCard service={service} />
            </Col>
          ))}
        </Row>
      </Container>
    </div>
  );
}

export default HomePage;