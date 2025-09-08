import React, { useState, useEffect, useRef } from "react";
import { TbEdit, TbTrash } from "react-icons/tb";
import { useDispatch } from "react-redux";
import { openModal } from "../../redux/features/modalSlice";
import { addSlipSingleUpdData } from "../../redux/features/SlipSingleUpdateSlice";
import { FaCommentSms, FaPrint, FaRegCopy } from "react-icons/fa6";
import { ipcRenderer } from "electron";
import { toast } from "react-toastify";
import moment from "moment";
import { useAppSelector } from "../../redux/config/hooks";
import { getSelectedPrinter } from "../../redux/features/printerListSlice";
import {
  getFontSize,
  getFooterText,
  selectShowPrintTime,
  selectTwoColumn,
  selectShowTermName,
  selectShowBusinessName,
  selectShowEmployeeName,
  selectShowSummary,
} from "../../redux/features/settingsSlice";
import { printBuyingList } from "../Print";
import { getUserProfile } from "../../redux/features/userProfileSlice";
import { sendSms } from "../Print/sendSms";
import { selectPrintWidth } from "../../redux/features/settingsSlice";
import Swal from "sweetalert2";
import { FaViber } from "react-icons/fa";

interface Item {
  _id: string;
  number: string;
  type: string;
  amount: number;
  slipId: string;
  termId: number;
  userId: number;
  summary: string;
  showSummary:string;
}

interface UserProfile {
  businessName: string;
  businessId: string;
  userId: string;
}

interface Term {
  value: string;
  label: string;
}

interface SlipDetailProps {
  copy: string;
  smsCopy: string;
  slipNumber: string;
  customerName: string;
  totalAmount: number;
  items: Item[];
  status: string;
  term: Term[];
  termId: string;
  phoneNumber: string;
  userId: string;
  userRole: string;
  userAccess:string;
  onDelete: (index: number, item: Item) => void;
  slipRefresh: (index: number) => void;
}

const SlipDetail: React.FC<SlipDetailProps> = ({
  copy,
  smsCopy,
  slipNumber,
  customerName,
  totalAmount,
  items,
  status,
  term,
  termId,
  phoneNumber,
  userId,
  onDelete,
  userRole,
  slipRefresh,
  userAccess
}) => {
  const dispatch = useDispatch();
  const [selectedRowIndex, setSelectedRowIndex] = useState<number>(-1);
  const rowRefs = useRef<(HTMLTableRowElement | null)[]>([]);
  const userProfile = useAppSelector(getUserProfile) as UserProfile;
  const printWidth = useAppSelector(selectPrintWidth);
  const printerName = useAppSelector(getSelectedPrinter);
  const isTwoColumn = useAppSelector(selectTwoColumn);
  const footerText = useAppSelector(getFooterText);
  const fontSize = useAppSelector(getFontSize);
  const showSummary = useAppSelector(selectShowSummary);
  const showPrintTime = useAppSelector(selectShowPrintTime);
  const showBusinessName = useAppSelector(selectShowBusinessName);
  const showEmployeeName = useAppSelector(selectShowEmployeeName);
  const showTermName = useAppSelector(selectShowTermName);
  const handleUpdate = (index: number, item: Item) => {
    dispatch(openModal("update-slip"));
    dispatch(addSlipSingleUpdData(item));
  };

  const handleDeleteClick = (index: number, item: Item) => {
    Swal.fire({
      // title: "Are you sure?",
      text: ` ${item.number} : ${item.amount} ကို ဖျက်မှာသေချာပါသလား?`,
      icon: "warning",
      showCancelButton: true,
      confirmButtonColor: "#d33",
      cancelButtonColor: "#3085d6",
      confirmButtonText: "Yes, delete it!",
      focusConfirm: true,
      returnFocus: false,
    }).then((result) => {
      if (result.isConfirmed) {
        onDelete(index, item);
      }
    });
  };

  // Handle keyboard navigation
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (items.length === 0) return;

      switch (event.key) {
        case 'ArrowDown':
          event.preventDefault();
          setSelectedRowIndex(prev => {
            const newIndex = prev < items.length - 1 ? prev + 1 : prev;
            // Scroll to the selected row
            setTimeout(() => {
              if (rowRefs.current[newIndex]) {
                rowRefs.current[newIndex]?.scrollIntoView({
                  behavior: 'smooth',
                  block: 'nearest'
                });
              }
            }, 0);
            return newIndex;
          });
          break;
        case 'ArrowUp':
          event.preventDefault();
          setSelectedRowIndex(prev => {
            const newIndex = prev > 0 ? prev - 1 : prev;
            // Scroll to the selected row
            setTimeout(() => {
              if (rowRefs.current[newIndex]) {
                rowRefs.current[newIndex]?.scrollIntoView({
                  behavior: 'smooth',
                  block: 'nearest'
                });
              }
            }, 0);
            return newIndex;
          });
          break;
        case 'Enter':
          event.preventDefault();
          if (selectedRowIndex >= 0 && selectedRowIndex < items.length) {
            handleUpdate(selectedRowIndex, items[selectedRowIndex]);
          }
          break;
        case 'Delete':
          event.preventDefault();
          if (selectedRowIndex >= 0 && selectedRowIndex < items.length) {
            handleDeleteClick(selectedRowIndex, items[selectedRowIndex]);
          }
          break;
      }
    };

    // Add event listener
    window.addEventListener('keydown', handleKeyDown);

    // Cleanup
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [items, selectedRowIndex]);

  // Reset selected row when items change
  useEffect(() => {
    // Reset refs array when items change
    rowRefs.current = new Array(items.length).fill(null);
    // Default select first row if items exist
    setSelectedRowIndex(items.length > 0 ? 0 : -1);
  }, [items]);

  return (
    <div className="bg-white p-6 rounded h-screen overflow-y-auto">
      {/* <h1 className="text-xl font-bold mb-4">စလစ်အသေးစိတ်</h1> */}
      <div className="text-xs font-bold space-y-1">
          <p>စလစ်နံပါတ်: {slipNumber}</p>
          <p>Print Copy [ {copy} ]</p>
          <p>SMS Copy [ {smsCopy} ]</p>
          <p>ထိုးသား: {customerName}</p>
          <p>ယူနစ်ပေါင်း : {totalAmount.toLocaleString()}</p>
        </div>

      <div className="flex justify-end space-x-2 mb-4">
        <button
          className="px-2 py-2 bg-blue-500 text-white rounded hover:bg-blue-600"
          data-tooltip-id={`copy`}
          data-tooltip-content="ကူးယူမယ်"
          onClick={() => {
            let businessName = showBusinessName
              ? `${userProfile?.businessName}\n`
              : "";
            let termName = showTermName
              ? `${term?.find((t: Term) => t.value === termId)?.label || ""}\n`
              : "";
            let slipId = `Slip No: ${slipNumber}\n`;
            // showEmployeeName ? {
            //   type: "text",
            //   value: employeeName,
            //   style: { margin: "0 0 0 0", fontWeight: "500", fontSize: `${fontSize}px` },
            // } : null,
            let formattedContent = "";
            const rows = items.map((item) => `${item.number}-${item.amount}`);
            if (isTwoColumn) {
              for (let i = 0; i < rows.length; i += 2) {
                const row1 = rows[i] || "";
                const row2 = rows[i + 1] || "";
                formattedContent += `${row1} | ${row2}\n`;
              }
            } else {
              for (let i = 0; i < rows.length; i++) {
                formattedContent += `${rows[i]}\n`;
              }
            }
            let totalAmount = items.reduce(
              (acc, item) => acc + Number(item.amount),
              0
            );
            let sms = `${businessName}${termName}${slipId}${formattedContent}\nTotal - ${totalAmount.toLocaleString()}`;
            navigator.clipboard
              .writeText(`${sms}`)
              .then(() => console.log("Copied to clipboard!"))
              .catch(() => console.log("Failed to copy row data."));
            ipcRenderer.send("open-viber");
          }}
        >
          <FaViber />
        </button>
        <button
          className="px-2 py-2 bg-blue-500 text-white rounded hover:bg-blue-600"
          data-tooltip-id={`copy`}
          data-tooltip-content="ကူးယူမယ်"
          onClick={() => {
            let businessName = showBusinessName
              ? `${userProfile?.businessName}\n`
              : "";
            let termName = showTermName
              ? `${term?.find((t: Term) => t.value === termId)?.label || ""}\n`
              : "";
            let slipId = `Slip No: ${slipNumber}\n`;
            // showEmployeeName ? {
            //   type: "text",
            //   value: employeeName,
            //   style: { margin: "0 0 0 0", fontWeight: "500", fontSize: `${fontSize}px` },
            // } : null,
            let formattedContent = "";
            const rows = items.map((item) => `${item.number}-${item.amount}`);
            if (isTwoColumn) {
              for (let i = 0; i < rows.length; i += 2) {
                const row1 = rows[i] || "";
                const row2 = rows[i + 1] || "";
                formattedContent += `${row1} | ${row2}\n`;
              }
            } else {
              for (let i = 0; i < rows.length; i++) {
                formattedContent += `${rows[i]}\n`;
              }
            }
            let totalAmount = items.reduce(
              (acc, item) => acc + Number(item.amount),
              0
            );
            let sms = `${businessName}${termName}${slipId}${formattedContent}\nTotal - ${totalAmount.toLocaleString()}`;
            navigator.clipboard
              .writeText(`${sms}`)
              .then(() => console.log("Copied to clipboard!"))
              .catch(() => console.log("Failed to copy row data."));
          }}
        >
          <FaRegCopy />
        </button>
        <button
          className="px-2 py-2 bg-green-500 text-white rounded hover:bg-green-600"
          data-tooltip-id={`print`}
          data-tooltip-content="ပရင့်ထုတ်မယ်"
          // onClick={printSlip}
          onClick={() => {
            printBuyingList({
              list: items,
              isTwoColumn,
              printerName,
              showBusinessName,
              businessName: userProfile?.businessName,
              showEmployeeName,
              employeeName: customerName,
              showTermName,
              termName:
                term?.find((t: Term) => t.value === termId)?.label || "",
              showPrintTime,
              footerText,
              fontSize,
              slipId: slipNumber,
              printWidth,
              copy: parseInt(copy) + 1,
              termId,
              userId: userId,
              businessId: userProfile?.businessId,
              showSummary,
            });
            setTimeout(() => slipRefresh(Math.random()), 300);
          }}
        >
          <FaPrint />
        </button>
        <button
          className="px-2 py-2 bg-purple-500 text-white rounded hover:bg-purple-600"
          data-tooltip-id={`sms`}
          data-tooltip-content="sms ပို့မယ်"
          onClick={() => {
            sendSms({
              list: items,
              isTwoColumn,
              slipId: slipNumber,
              phoneNumber: phoneNumber || "",
              termId,
              userId: userId,
              copy: parseInt(smsCopy) + 1,
            });
            setTimeout(() => slipRefresh(Math.random()), 1000);
          }}
        >
          <FaCommentSms />
        </button>
      </div>

      <table className="table-auto w-full border-collapse border border-gray-300">
        <thead>
          <tr className="bg-gray-200">
            <th
              className="border border-gray-400 px-4 py-2 text-center text-lg"
              style={{ width: "80px" }}
            >
              နံပါတ်
            </th>
            <th
              className="border border-gray-400 px-4 py-2 text-right text-lg"
              style={{ width: "150px" }}
            >
              ယူနစ်
            </th>
            <th
              className="border border-gray-400 px-4 py-2 text-center text-lg"
              style={{ width: "150px" }}
            >
              Summary
            </th>
            {userRole &&(userRole=='owner' ||(userRole === "employee" && userAccess === "1")) &&(
              <th
                className="border border-gray-400 px-4 py-2 text-center"
                style={{ width: "80px" }}
              ></th>
            )}
          </tr>
        </thead>
        <tbody>
          {items.map((item, index) => (
            <tr 
              key={index}
              ref={(el) => (rowRefs.current[index] = el)}
              className={`border border-gray-400 cursor-pointer transition-colors ${
                index === selectedRowIndex 
                  ? 'bg-blue-100 border-blue-500' 
                  : 'hover:bg-gray-50'
              }`}
              onClick={() => setSelectedRowIndex(index)}
            >
              <td className="border border-gray-400 px-4 py-2 text-center text-lg">
                {item.number}
              </td>
              <td className="border border-gray-400 px-4 py-2 text-right text-lg">
                {item.amount.toLocaleString()}
              </td>
              <td className="border border-gray-400 px-4 py-2 text-center text-lg">
                {item.showSummary=='1'?item.summary:''}
              </td>
              {userRole && (userRole=='owner' ||(userRole === "employee" && userAccess === "1")) && (
                <td className="border border-gray-200 px-4 py-2 text-center flex flex-row">
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      handleUpdate(index, item);
                    }}
                    className="text-blue-500 hover:text-blue-700 mx-2"
                    title="Edit"
                  >
                    <TbEdit size={18} />
                  </button>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      handleDeleteClick(index, item);
                    }}
                    className="text-red-500 hover:text-red-700 mx-2"
                    title="Delete"
                  >
                    <TbTrash size={18} />
                  </button>
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>

      <div className="mt-4 text-right">
        <p className="text-lg font-bold">
          ယူနစ်ပေါင်း :{" "}
          {items
            .reduce((sum, detail) => sum + detail.amount, 0)
            .toLocaleString()}{" "}
        </p>
      </div>

      {/* Keyboard shortcuts help */}
      <div className="mt-4 p-3 bg-gray-100 rounded text-sm">
        <p className="font-semibold mb-2">Keyboard Shortcuts:</p>
        <div className="grid grid-cols-2 gap-2 text-xs">
          <span>↑/↓ - Navigate rows</span>
          <span>Enter - Edit selected row</span>
          <span>Delete - Delete selected row</span>
          <span>Click - Select row</span>
        </div>
      </div>
    </div>
  );
};

export default SlipDetail;
