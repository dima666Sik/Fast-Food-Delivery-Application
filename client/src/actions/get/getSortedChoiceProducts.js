import axios from "axios";

export const getSortedChoiceProducts = async (sortedChoice) => {
	try {
		const response = await axios.get(
			`${process.env.REACT_APP_SERVER_API_URL}api/v2/product/foods/sort/${sortedChoice}`
		);

		const updatedProducts = response.data.map((product) => {
			return {
				...product,
				image01: `${process.env.REACT_APP_SERVER_API_URL}public/images/products/${product.image01}`,
				image02: `${process.env.REACT_APP_SERVER_API_URL}public/images/products/${product.image02}`,
				image03: `${process.env.REACT_APP_SERVER_API_URL}public/images/products/${product.image03}`,
			};
		});
		return updatedProducts;
	} catch (error) {
		console.log(error.response.data.message);
		throw error;
	}
};
