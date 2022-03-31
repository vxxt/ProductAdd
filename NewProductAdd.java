package extra02elevation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Scanner;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * A set of methods to automate adding products to the elevationsupply website
 * 
 * @author Viet Nguyen | Jun 2018
 *
 */
public class NewProductAdd {

	private final static String MANUFACTURER = "Accuris Instruments";
	private static WebDriver drive = null;
	private static String[][] data = null;
	private static HashSet<String> set = null;

	public static void main(String[] args) throws InterruptedException, FileNotFoundException {
		populateSet();
		bootUp();
		scanCSV();
		autofillPage();

		System.out.println("DONE!");
	}

	/**
	 * A helper method to populate the set with the data from previous.txt
	 */
	private static void populateSet() {
		set = new HashSet<String>();
		File file = new File("src/extra02elevation/previous.txt");
		try (Scanner scan = new Scanner(file)) {
			while (scan.hasNextLine()) {
				String line = scan.nextLine().replaceAll("[, ,\\\\u00a0]", "");
				set.add(line);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Helper method to run the autofilling process based off the information found
	 * in the String[][] data
	 * 
	 * @throws InterruptedException
	 */
	private static void autofillPage() throws InterruptedException {
		try (BufferedWriter write = new BufferedWriter(new FileWriter("src/extra02elevation/previous.txt", true))) {
			for (int i = 0; i < data.length; i++) {
				//
				//
				// THE LOCAITIONS IN THE ROW FOR EACH SPECIFICATION (i.e sku, price, cost,
				// weight) IS HARDCODED IN; CHANGE THIS TO BE MORE GENERIC OR CHANGE
				// WHENEVER IMPLEMENTING A DIFFERENT VENDOR.
				//
				// ALSO IT MAY BE IMPORTANT TO DISABLE ECHECKING IN FILECHECK WHILE
				// LOOKING FOR AN IMAGE TO UPLOAD
				//
				//
				String name = data[i][0].replaceAll("[ ,\\u00a0]", "");
				String price = data[i][2].replaceAll("[$, ]", "");
				String cost = data[i][4].replaceAll("[$, ]", "");
				String weight = data[i][10];
				while (!drive.getCurrentUrl()
						.equals("https://www.elevationsupply.com/admin/shopping_cart/products?login=")
						&& !drive.getCurrentUrl()
								.equals("https://www.elevationsupply.com/admin/shopping_cart/products?list")) {
					Thread.sleep(100);
				}
				drive.findElement(By.className("btn_link")).click();
				String productName = "" + name + ", " + data[i][1];
				drive.findElement(By.name("new_product")).sendKeys(productName);
				drive.findElement(By.xpath("//*[@id=\"new_product_box_form\"]/button")).click();

				// Thread.sleep(1000);

				// Clipboard clippy = Toolkit.getDefaultToolkit().getSystemClipboard(); // Miss
				// u Clippy <3
				// StringSelection selection = new StringSelection(name);
				// clippy.setContents(selection, null);

				// TODO: Wait until page loads instead of Thread.sleep();
				WebDriverWait wait = new WebDriverWait(drive, 1000);
				wait.until((ExpectedCondition<Boolean>) drive -> drive.getCurrentUrl().contains("products?prodedit="));

				clearAndWriteByName("sku", name);
				clearAndWriteByName("manufacturer", MANUFACTURER);
				Thread.sleep(100);
				clearAndWriteByName("cost", price);
				clearAndWriteByName("our_cost", cost);
				clearAndWriteByName("retail_cost", price);
				clearAndWriteByName("weight", weight);
				drive.findElement(By.xpath("//*[@id=\"details-tab\"]/div[2]/div[15]/div/div[2]/input")).click();
				drive.findElement(By.xpath("//*[@id=\"details-tab\"]/div[2]/div[15]/div/div[2]/a")).click();
				drive.findElement(By.xpath("//*[@id=\"details-tab\"]/div[2]/div[15]/div/div[2]/div/div[16]/input")).click();
				write.write(name);
				write.newLine();
				write.flush();

				wait.until((ExpectedCondition<Boolean>) drive -> isCorrectlyFormatted(
						drive.findElement(By.name("model")).getAttribute("value")));
				uploadImage(name);
				System.out.print("\n# products entered: ");
				System.out.println(i + 1);

				// Auto click
				// drive.findElement(By.xpath("//*[@id=\"content\"]/div[2]/div/div[3]/button")).click();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * A helper method to upload the image based of the sku given. If an image
	 * contains the sku in its filename it will select it for upload. Otherwise, it
	 * will open the box for manual user selection.
	 * 
	 * @param sku
	 *            - The String representing the sku to search for in the filenames
	 */
	private static void uploadImage(String sku) {
		drive.findElement(By.xpath("//*[@id=\"ntab_f\"]/a")).click();
		String id = drive.getCurrentUrl().split("=")[1].split("&")[0];
		drive.findElement(By.cssSelector("#files_" + id + " > div > span > button")).click();
		drive.findElement(By.cssSelector("#file_upload_fields_" + id + " > div > input[type=\"radio\"]:nth-child(3)"))
				.click();

		String imagePath = fileCheck(sku);

		// If the image exists in the folder, upload it as the main photo; otherwise,
		// just open the dialogue box
		if (imagePath != null) {
			drive.findElement(By.xpath("//*[@id=\"file_upload_fields_" + id + "\"]/table/tbody/tr[1]/td[2]/input"))
					.sendKeys(imagePath);
			drive.findElement(By.cssSelector(
					"#file_upload_fields_" + id + "> table > tbody > tr:nth-child(5) > td:nth-child(2) > button"))
					.click();
		} else {
			drive.findElement(By.xpath("//*[@id=\"file_upload_fields_" + id + "\"]/table/tbody/tr[1]/td[2]/input"))
					.click();
		}
	}

	/**
	 * A small helper method that checks if a file exists in Downloads > Bench
	 * containing the given String. Will perform as expected if there are files
	 * existent in the given folder.
	 * 
	 * @param sku
	 *            - The sku number of the current product
	 * @return
	 */
	private static String fileCheck(String sku) {
		File benchDir = new File("/Users/HalfNam/Downloads/Accuris/");
		File[] allFiles = benchDir.listFiles();

		// Binary search might be a better way since everything is sorted already
		for (File f : allFiles) {
			String filePath = f.getName();
			if (filePath.contains(sku)) {
				return f.getPath();
			}
			if (eChecker(sku) != null && filePath.contains(eChecker(sku))) {
				return f.getPath();
			}

		}
		return null;

	}

	/**
	 * Helper method to check if a string ends in "-E" or "E" in reference to it
	 * being a European part number
	 * 
	 * @param word
	 * @return
	 */
	private static String eChecker(String word) {
		if (word.endsWith("E")) {
			if (word.endsWith("-E")) {
				return word.substring(0, word.length() - 2);
			} else if (word.endsWith("E")) {
				return word.substring(0, word.length() - 1);
			}
		}
		return null;
	}

	/**
	 * Helper method to scan the .csv file and put it into the 2d array
	 */
	private static void scanCSV() throws FileNotFoundException {
		File products = new File("src/extra02elevation/current.txt");
		Scanner scan = null;
		try {
			scan = new Scanner(products);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		int rows = 869;
		int cols = 14;
		data = csvTo2DArray(scan, rows, cols);
		System.out.println(".csv scan sucessful...");
	}

	/**
	 * Helper method to start the driver and log in to the site
	 */
	private static void bootUp() {
		String os = System.getProperty("os.name").toLowerCase();
		if (os.contains("mac")) {
			System.setProperty("webdriver.chrome.driver",
					System.getProperty("user.dir") + "/src/extra03catering/chromedriver");
		} else {
			System.setProperty("webdriver.chrome.driver",
					System.getProperty("user.dir") + "\\src\\extra03catering\\chromedriver.exe");
		}
		drive = new ChromeDriver();
		drive.get("https://www.elevationsupply.com/admin/shopping_cart/products");
		drive.findElement(By.name("email")).sendKeys("user"); // TODO: ENTER USER
		drive.findElement(By.name("password")).sendKeys("password"); // TODO: ENTER PASS
		drive.findElement(By.xpath("//*[@id=\"login\"]/form/button")).click();
		System.out.println("Log in sucessful...");
	}

	/**
	 * Helper method to clear an element of preexisting text and fill it with the
	 * text specified by the user
	 * 
	 * @param name
	 * @param keys
	 */
	static void clearAndWriteByName(String name, String keys) {
		WebElement element = drive.findElement(By.name(name));
		element.clear();
		element.sendKeys(keys);
	}

	/**
	 * Helper method to turn a .csv file to a 2D array to use in finding and
	 * replacing prices
	 * 
	 * @param file
	 * @return
	 */
	static String[][] csvTo2DArray(Scanner file, int rows, int cols) {
		String[][] arr = new String[rows][cols];
		int row = 0;
		while (file.hasNextLine()) {
			String[] line = file.nextLine().split("\t");
			if (line.length > 0) {
				String name = line[0].replaceAll("[ ,\\\\u00a0]", "");
				if (line.length == 14 && !set.contains(name) && !line[13].isEmpty() && !name.equals("Item No.")) {
					arr[row++] = line;
					set.add(name);
				} else if (set.contains(name)) {
					System.out.println("DUPLICATE FOUND: " + name);
				}
			}
		}
		return arr;
	}

	/**
	 * Helper method to check if the string is formatted correctly. Checking length
	 * for 2 characters. All capital letters. No numbers or special characters
	 * present.
	 * 
	 * @param word
	 *            - The word to check the format of
	 * @return
	 */
	static boolean isCorrectlyFormatted(String word) {
		if (word.length() != 2) {
			return false;
		}
		for (int i = 0; i < word.length(); i++) {
			if (!Character.isLetter(word.charAt(i)) || !Character.isUpperCase(word.charAt(i))) {
				return false;
			}
		}
		return true;
	}
}
