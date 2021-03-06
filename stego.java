import java.io.*;
import java.nio.charset.StandardCharsets;

public class stego
{

	/**
	 * A constant to hold the number of bits per byte
	 */
	private final int byteLength=8;

	/**
	 * A constant to hold the number of bits used to store the size of the file extracted
	 */
	protected final int sizeBitsLength=32;
	/**
	 * A constant to hold the number of bits used to store the extension of the file extracted
	 */
	protected final int extBitsLength=64;
	/**
	 * A constant to hold the number of bits used to store the header size of a bmp
	 */
	protected final int sizeHeaderLength=54;

	 /**
	Default constructor to create a steg object, doesn't do anything - so we actually don't need to declare it explicitly. Oh well.
	*/

	public void Steg()
	{

	}

	/**
	A method for hiding a string in an uncompressed image file such as a .bmp or .png
	You can assume a .bmp will be used
	@param cover_filename - the filename of the cover image as a string
	@param payload - the string which should be hidden in the cover image.
	@return a string which either contains 'Fail' or the name of the stego image which has been
	written out as a result of the successful hiding operation.
	You can assume that the images are all in the same directory as the java files
	*/
	//TODO you must write this method
	public String hideString(String payload, String cover_filename)
	{
		//setup file object for getting the size of it
		File cf = new File(cover_filename);
		long cf_lsb_can_hide = cf.length() - sizeBitsLength - sizeHeaderLength;
		byte[] payload_bytes = payload.getBytes(StandardCharsets.US_ASCII);
		int bytes_to_hide = payload_bytes.length;
		int bits_to_hide = bytes_to_hide*byteLength;
		int curr_byte2hide = 0;
		int curr_bit2hide = 0;
		boolean message_hidden = false;

		//if trying to hide more bits than available lsb's, stop
		if (bits_to_hide > cf_lsb_can_hide){
			return "Fail: payload was too big for cover file";
		}

		// Load the cover image on an input stream.
		InputStream coverim;
		int cover_image;
		try {
			coverim = new FileInputStream(cover_filename);
		} catch (IOException e){
			return "Fail: couldn't load cover image.";
		}

		//set up the new file
		File file_mod = null;
		try {
			file_mod = gen_mod_file(cover_filename);
		} catch (IOException e) {
			return "Fail: couldn't create file for output.";
		}

		//try to set up the output stream
		FileOutputStream coverim_mod;
		try {
			coverim_mod = new FileOutputStream(file_mod);
		} catch (IOException e){
			return "Fail: some issue creating output stream.";
		}

		// Determine the size of the payload to be hidden
		String payload_size = left_pad_zeroes(bytes_to_hide, sizeBitsLength);
		int curr_char_pls = 0;
		for(int i = 0; i < cf.length(); i++){
			try {
				cover_image = coverim.read();

				if (i < sizeHeaderLength){
					coverim_mod.write(cover_image);

				}
				else if (i >= sizeHeaderLength && i < (sizeHeaderLength + sizeBitsLength)){
					coverim_mod.write(swapLsb(Character.getNumericValue(payload_size.charAt(curr_char_pls)),cover_image));
					curr_char_pls++;
				}
				else {
					if(curr_byte2hide == bytes_to_hide){
						message_hidden = true;
					}
					if (message_hidden){
						coverim_mod.write(cover_image);
					} else {
						int curr_pBitVal = bit_shifter(payload_bytes[curr_byte2hide],curr_bit2hide%8);
						coverim_mod.write(swapLsb(curr_pBitVal, cover_image));
						curr_bit2hide = curr_bit2hide + 1;
						if (curr_bit2hide%byteLength == 0){
							curr_byte2hide += 1;
						}
					}
				}
			} catch (IOException e){
				return "Fail: unable to move past header of image file.";
			}
		}
		//Close off the input and output streams
		try{
			coverim_mod.flush();
			coverim_mod.close();
			coverim.close();
		} catch(IOException e){
			return "Fail: couldn't close off streams.";
		}

		return file_mod.getName();
	}
	//TODO you must write this method
	/**
	The extractString method should extract a string which has been hidden in the stegoimage
	@param stego_image - the name of the stego image
	@return a string which contains either the message which has been extracted or 'Fail' which indicates the extraction
	was unsuccessful
	*/
	public String extractString(String stego_image)
	{
		// Load the cover image on an input stream.
		InputStream stegim;
		try {
			stegim = new FileInputStream(stego_image);
		} catch (IOException e){
			return "Fail: couldn't load stego image.";
		}

		//Extracting the size of payload
		String messageSize = "";
		int steg_image;
		for(int i = 0; i < (sizeHeaderLength + sizeBitsLength); i++){
			try {
				steg_image = stegim.read();
				if (i >= sizeHeaderLength){
					messageSize += (steg_image%2);
				}
			}catch (IOException e){
				return "Fail: unable to move past header of image file.";
			}
		}

		//extraction of payload
		int payLoadSize = Integer.parseInt(messageSize, 2);//get int rep of string
		String message = "";
		String binMessage = "";
		for(int i = 0; i < payLoadSize * byteLength; i++){
			try{
				steg_image = stegim.read();
				binMessage = (steg_image%2) + binMessage;
				if(binMessage.length() == 8){
					int charcode = Integer.parseInt(binMessage, 2);
					message += (char)charcode;
					binMessage = "";
				}
			} catch (IOException e){
				return "Fail: unable to read image.";
			}
		}
		return message;
	}

	//TODO you must write this method
	/**
	The hideFile method hides any file (so long as there's enough capacity in the image file) in a cover image

	@param file_payload - the name of the file to be hidden, you can assume it is in the same directory as the program
	@param cover_image - the name of the cover image file, you can assume it is in the same directory as the program
	@return String - either 'Fail' to indicate an error in the hiding process, or the name of the stego image written out as a
	result of the successful hiding process
	*/
	public String hideFile(String file_payload, String cover_image)
	{
		File cf = new File(cover_image);
		FileReader fr_payload = new FileReader(file_payload);

		//checking sufficient space to hide the payload
		int lsbs_available = (int)cf.length() - ((sizeHeaderLength + sizeBitsLength + extBitsLength)/byteLength);
		if(lsbs_available < fr_payload.getFileSize()){
			return "Fail: Payload too big to hide!";
		}

		// Load the cover image on an input stream.
		InputStream coverim;
		try {
			coverim = new FileInputStream(cover_image);
		} catch (IOException e){
			return "Fail: couldn't load cover image.";
		}

		//try to set up the new file
		File file_mod = null;
		try {
			file_mod = gen_mod_file(cover_image);
		} catch (IOException e) {
			return "Fail: couldn't set up output file.";
		}

		//try to set up the output stream
		FileOutputStream coverim_mod;
		try {
			coverim_mod = new FileOutputStream(file_mod);
		} catch (IOException e){
			return "Fail: issue creating output stream.";
		}

		//begin hiding the payload(file)
		int bit_count = 0;
		int curr_byte;
		for (int i = 0; i < cf.length(); i++){
			try {
				curr_byte = coverim.read();
			} catch (IOException e) {
				return "Fail: issue reading the cover image";
			}
			//if still in header, write unmodified bytes
			if (bit_count < sizeHeaderLength){
				try {
					coverim_mod.write(curr_byte);
				} catch (IOException e) {
					return "Fail: unable to move past header of image file.";
				}

			}
			// if still in size write, modify cover image with fileReader sBits
			else if (bit_count < (sizeHeaderLength + sizeBitsLength)){
				int curr_bit_to_hide = fr_payload.getNextBit();
				try {
					coverim_mod.write(swapLsb(curr_bit_to_hide, curr_byte));
				} catch (IOException e) {
					return "Fail: couldn't write file size for payload";
				}
			}
			// if still in extension write, modify cover image with fileReader extBits
			else if (bit_count < (sizeHeaderLength + sizeBitsLength + extBitsLength)){
				int curr_bit_to_hide = fr_payload.getNextBit();
				try {
					coverim_mod.write(swapLsb(curr_bit_to_hide, curr_byte));
				} catch (IOException e) {
					return "Fail: couldn't write file size for payload";
				}
			}
			//if still in payload write, modify cover image with payload(file) bits
			else {
				//only modify if there are still bits of the payload to hide
				if (fr_payload.hasNextBit()){
					int curr_bit_to_hide = fr_payload.getNextBit();
					try{
						coverim_mod.write(swapLsb(curr_bit_to_hide, curr_byte));
					}catch (IOException e){
						return "Fail: couldn't hide file bit in image byte";
					}
				}
				//otherwise, write bytes unmodified
				else {
					try{
						coverim_mod.write(curr_byte);
					}catch (IOException e){
						return "Fail: couldn't write current file byte";
					}
				}
			}
			//increment the bit count
			bit_count++;
		}

		//Close off the input and output streams
		try{
			coverim_mod.flush();
			coverim_mod.close();
			coverim.close();
		} catch(IOException e){
			return "Fail: couldn't close off streams.";
		}
		return file_mod.getName();
	}

	//TODO you must write this method
	/**
	The extractFile method hides any file (so long as there's enough capacity in the image file) in a cover image

	@param stego_image - the name of the file to be hidden, you can assume it is in the same directory as the program
	@return String - either 'Fail' to indicate an error in the extraction process, or the name of the file written out as a
	result of the successful extraction process
	*/
	public String extractFile(String stego_image)
	{
		InputStream is_stego_img;
		//try to setup input stream to read modified file
		try {
			is_stego_img = new FileInputStream(stego_image);
		} catch (FileNotFoundException e) {
			return "Fail: couldn't set up the input stream";
		}


		int curr_byte;
		String extracted_messageSize = "";
		String extracted_extension = "";
		//for loop to extract size and extension of hidden file
		for(int i = 0; i < (sizeHeaderLength + sizeBitsLength + extBitsLength); i++){
			try {
				curr_byte = is_stego_img.read();
			} catch (IOException e) {
				return "Fail: couldn't read from input stream";
			}
			//extraction of size
			if (i >= sizeHeaderLength && i < (sizeHeaderLength + sizeBitsLength)){
				extracted_messageSize += (curr_byte%2);
			}
			//extraction of extension
			else if (i >= (sizeHeaderLength + sizeBitsLength)){
				extracted_extension += (curr_byte%2);
			}
		}

		//process file size & extension
		int hf_size = Integer.parseInt(extracted_messageSize, 2); //hidden file's extension
		String hf_ext = convert_bin_2_ext(extracted_extension); //hidden file's size

		//Attempt to set up the File output stream
		String extracted_file_name;
		OutputStream os_hidden_file;
		try {
			extracted_file_name = gen_extract_file(hf_ext);
			os_hidden_file = new FileOutputStream(extracted_file_name);
		} catch (IOException e) {
			return "Fail: unable to create FileOutputStream";
		}

		String curr_byte_string = "";
		for (int i = 0; i < hf_size * byteLength; i++){
			try {
				curr_byte = is_stego_img.read();
			} catch (IOException e) {
				return "Fail: unable to read current file byte";
			}
			curr_byte_string = (curr_byte%2) + curr_byte_string;
			if(curr_byte_string.length() == 8){
				int curr_byte_int = Integer.parseInt(curr_byte_string, 2);
				try {
					os_hidden_file.write(curr_byte_int);
				} catch (IOException e) {
					return "Fail: unable to write extracted file byte";
				}
				curr_byte_string = "";
			}
		}
		//Close off the input and output streams
		try{
			os_hidden_file.flush();
			os_hidden_file.close();
			is_stego_img.close();
		} catch(IOException e){
			return "Fail: couldn't close off streams.";
		}

		return extracted_file_name;
	}

	//method for extracting the file extension from binary string
	private String convert_bin_2_ext(String extracted_extension) {
		String message = "";
		String binMessage = "";
		for(int i = 0; i < extBitsLength; i++){
			binMessage += (extracted_extension.charAt(i)%2);// + binMessage;
			if(binMessage.length() == 8){
				int charcode = Integer.parseInt(binMessage, 2);
				if(charcode != 0){
					message += (char)charcode;
				}
				binMessage = "";
			}
		}
		return message;
	}

	//TODO you must write this method
	/**
	 * This method swaps the least significant bit with a bit from the filereader
	 * @param bitToHide - the bit which is to replace the lsb of the byte of the image
	 * @param byt - the current byte
	 * @return the altered byte
	 */
	public int swapLsb(int bitToHide,int byt)
	{
		return byt - byt%2 + bitToHide;
	}

	/*
	* determining if current bit to hide is 1 or 0
	* */
	public int bit_shifter(int byte_x,int bit_idx)
	{
		return ((byte_x) >> bit_idx)%2;
	}

	/*
	* help generate 32 bit left padded binary string
	* */
	public String left_pad_zeroes(int padee, int desired_len){

		String bin_str_short = Integer.toBinaryString(padee);
		int zeroes_to_pad = desired_len - bin_str_short.length();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < zeroes_to_pad; i++){
			sb.append("0");
		}
		sb.append(bin_str_short);
		return sb.toString();
	}

	private File gen_mod_file(String cover_image) throws IOException {
		File file_mod = null;
		int j = 0;
		boolean file_exists = true;
		String new_img_name;
		//find available file to save to
		while (file_exists){
			new_img_name = "" + j + cover_image;
			try{
				file_mod = new File(new_img_name);
				if (!file_mod.exists()){
					file_mod.createNewFile();
					file_exists = false;
				} else {
					j++;
				}
			}catch(IOException e){
				throw new IOException("Fail: couldn't create output file.");
			}

		}
		return file_mod;
	}

	private String gen_extract_file(String ext) throws IOException {
		String file_base_name = "_Extracted" + ext;
		File file_mod;
		int j = 0;
		boolean file_exists = true;
		String new_img_name = "";
		//find available file to save to
		while (file_exists){
			new_img_name = "" + j + file_base_name;
			try{
				file_mod = new File(new_img_name);
				if (!file_mod.exists()){
					file_mod.createNewFile();
					file_exists = false;
				} else {
					j++;
				}
			}catch(IOException e){
				return "Fail: couldn't create output file.";
			}
		}
		return new_img_name;
	}

}
