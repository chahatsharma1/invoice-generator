import { useState, useEffect } from "react";
import axios from "axios";

const FormField = ({ children, ...props }) => (
    <div className="form-field" {...props}>
        {children}
    </div>
);


export default function App() {
    const [dealers, setDealers] = useState([]);
    const [vehicles, setVehicles] = useState([]);
    const [dealerId, setDealerId] = useState("");
    const [vehicleId, setVehicleId] = useState("");
    const [customerName, setCustomerName] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchData = async () => {
            try {
                const [dealerRes, vehicleRes] = await Promise.all([
                    axios.get(`/api/dealers`),
                    axios.get(`/api/vehicles`),
                ]);
                setDealers(dealerRes.data);
                setVehicles(vehicleRes.data);
            } catch (err) {
                console.error("Error fetching dropdown data", err);
                setError("Could not load dealer/vehicle data.");
            }
        };
        fetchData();
    }, []);

    const generateInvoice = async (event) => {
        event.preventDefault();
        if (!dealerId || !vehicleId || !customerName) {
            setError("Please fill all fields before generating.");
            return;
        }
        setError(null);
        setLoading(true);

        try {
            const response = await axios.post(
                `/api/invoices/generate`,
                { dealerId, vehicleId, customerName },
                { responseType: "blob" }
            );

            const url = window.URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
            const link = document.createElement("a");
            link.href = url;
            link.setAttribute("download", `invoice-${Date.now()}.pdf`);
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(url);

        } catch (error) {
            console.error("Error generating invoice", error);
            setError("An error occurred while generating the invoice.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <main className="app-main">
            <form className="form-container" onSubmit={generateInvoice}>
                <div className="header">
                    <h1>Invoice Generator</h1>
                    <p>Create and download a professional sales invoice.</p>
                </div>

                <div className="fields-wrapper">
                    <FormField>
                        <select value={dealerId} onChange={(e) => setDealerId(e.target.value)} required>
                            <option value="">Select Dealer</option>
                            {dealers.map((dealer) => (
                                <option key={dealer.id} value={dealer.id}>
                                    {dealer.name}
                                </option>
                            ))}
                        </select>
                    </FormField>

                    <FormField>
                        <select value={vehicleId} onChange={(e) => setVehicleId(e.target.value)} required>
                            <option value="">Select Vehicle</option>
                            {vehicles.map((vehicle) => (
                                <option key={vehicle.id} value={vehicle.id}>
                                    {vehicle.make} {vehicle.model} - ₹{vehicle.price.toLocaleString('en-IN')}
                                </option>
                            ))}
                        </select>
                    </FormField>

                    <FormField>
                        <input
                            type="text"
                            placeholder="Customer Name"
                            value={customerName}
                            onChange={(e) => setCustomerName(e.target.value)}
                            required
                        />
                    </FormField>
                </div>

                {error && <div className="error-message">{error}</div>}

                <button
                    type="submit"
                    disabled={loading}
                    className="generate-button">
                    {loading ? "Generating..." : "Download Invoice"}
                </button>
            </form>
        </main>
    );
}