import axios from "axios";

export const addOrderPurchaseUser = async (
	accessToken,
	userShippingAddress
) => {
	try {
		const response = await axios.post(
			`${process.env.REACT_APP_SERVER_API_URL}api/v2/order-purchase/private/add-order-with-purchase-user`,
			userShippingAddress,
			{
				headers: {
					Authorization: "Bearer " + accessToken,
				},
			}
		);
		return response;
	} catch (error) {
		console.log(error.response.data.message);
		throw error;
	}
};

export const addOrderPurchaseGuest = async (userShippingAddress) => {
	try {
		const response = await axios.post(
			`${process.env.REACT_APP_SERVER_API_URL}api/v2/order-purchase/add-order-with-purchase-guest`,
			userShippingAddress
		);
		return response;
	} catch (error) {
		console.log(error.response.data.message);
		throw error;
	}
};
